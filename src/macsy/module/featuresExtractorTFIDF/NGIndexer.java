/**
 * NG-Indexer
 * 
 * Author: Ilias Flaounas
 * Last update 24-1-2010
 *  
 */

package macsy.module.featuresExtractorTFIDF;

import java.util.Map;
import java.util.TreeMap;


public class NGIndexer {

//	private Connection _scon=null;

	private NGPreprocessing _pre = null;

	public NGVoc _voc = null;


	/**
	 * Constructor 
	 * @param voc_filename  = The vocabulary to use
	 * @param stopwords_filename	=Stopwordsfile to use
	 * @throws Exception
	 */
	public NGIndexer(String voc_filename, String stopwords_filename) throws Exception {
		System.out.print("Reading Vocabulary and Stop words...");

		if(voc_filename==null)
			_voc=null;
		else
			_voc = new NGVoc( voc_filename );
		System.out.println("\t[ DONE ]");

		_pre = new NGPreprocessing(stopwords_filename);	
	}

	/**
	 * Return the next available Feature ID
	 * @return
	 */
	public int NextAvailableFeatureID()
	{
		return _voc.currentID+1;
	}

	/**
	 * It will perform
	 * - Stop word removal
	 * - Stemming
	 * - Indexing of stems
	 * - Calculation of TF-IDF (B-O-W) representation
	 * Output format: <word_id>:<TFIDF><space><word_id>:<TFIDF>
	 * 
	 * @param input
	 * @return BOW of input
	 */
	public String CreateBOW(String input) throws Exception
	{
		String Input_pre =  _pre.doPreprocess(input);

		Map<Integer, Integer> TF = String2TF(Input_pre); 

		if(_voc!=null)
			return Indexed2TFIDF(TF);
		return Indexed2TF(TF);
	}


	/**
	 * It will perform
	 * - Stop word removal
	 * - Stemming
	 * - Indexing of stems
	 * - Calculation of TF-IDF (B-O-W) representation
	 * Output format: <word_id>:<TFIDF><space><word_id>:<TFIDF>
	 * 
	 * @param input
	 * @return BOW of input
	 */
	public Map<Integer,Double> CreateBOW_Map(String input) throws Exception
	{
		String Input_pre =  _pre.doPreprocess(input);

		Map<Integer, Integer> TF = String2TF(Input_pre); 

		if(_voc!=null)
			return Indexed2TFIDF_Map(TF);

		return Indexed2TF_Map(TF);
	}

	/**
	 * Return a binary representation of input 
	 * 
	 * @param input
	 * @return
	 * @throws Exception
	 */
	public String CreateBinary(String input) throws Exception
	{
		String Input_pre =  _pre.doPreprocess(input);

		Map<Integer, Integer> TF = String2Bin(Input_pre); 


		StringBuffer Bin_featurevector=new StringBuffer(1000);
		//First pass calculate total number of words.
		for (Map.Entry<Integer, Integer> e : TF.entrySet()) 
			Bin_featurevector.append( e.getKey() + ":1.0 ");

		return Bin_featurevector.toString();
	}


	/**
	 * From TF(WordID-> Frequency) Map goes to TFIDF String
	 * Output format: <word_id>:<TFIDF><space><word_id>:<TFIDF>
	 * @param indexed
	 * @return
	 */
	private String Indexed2TFIDF(Map<Integer, Integer> indexed ) throws Exception {
		StringBuffer TFIDF_featurevector=new StringBuffer(1000);

		//First pass calculate total number of words.
		int NumOfTerms = 0;
		for (Map.Entry<Integer, Integer> e : indexed.entrySet()) 
			NumOfTerms+= e.getValue();


		for (Map.Entry<Integer, Integer> e : indexed.entrySet()) 
		{
			Integer WordID = e.getKey();
			double tfidf =_voc.getWordIDF(WordID) *  e.getValue() / (double)NumOfTerms;
			TFIDF_featurevector.append(WordID + ":"+ tfidf +" ");
		}

		return TFIDF_featurevector.toString();
	}

	private String Indexed2TF(Map<Integer, Integer> indexed ) throws Exception {
		StringBuffer TFIDF_featurevector=new StringBuffer(1000);

		//First pass calculate total number of words.
		int NumOfTerms = 0;
		for (Map.Entry<Integer, Integer> e : indexed.entrySet()) 
			NumOfTerms+= e.getValue();


		for (Map.Entry<Integer, Integer> e : indexed.entrySet()) 
		{
			Integer WordID = e.getKey();
			double tfidf = e.getValue() / (double)NumOfTerms;
			TFIDF_featurevector.append(WordID + ":"+ tfidf +" ");
		}

		return TFIDF_featurevector.toString();
	}
	
	/**
	 * From TF(WordID-> Frequency) Map goes to TFIDF String
	 * Output format: <word_id>:<TFIDF><space><word_id>:<TFIDF>
	 * @param indexed
	 * @return
	 */
	private Map<Integer,Double> Indexed2TFIDF_Map(Map<Integer, Integer> indexed ) throws Exception {
		Map<Integer,Double> TFIDF_featurevector=new TreeMap<Integer,Double>();

		//First pass calculate total number of words.
		int NumOfTerms = 0;
		for (Map.Entry<Integer, Integer> e : indexed.entrySet()) 
			NumOfTerms+= e.getValue();

		//Second pass 
		for (Map.Entry<Integer, Integer> e : indexed.entrySet()) 
		{
			Integer WordID = e.getKey();
			double tfidf =_voc.getWordIDF(WordID) *  e.getValue() / (double)NumOfTerms;
			TFIDF_featurevector.put(WordID , tfidf);
		}

		return TFIDF_featurevector;
	}

	private Map<Integer,Double> Indexed2TF_Map(Map<Integer, Integer> indexed ) throws Exception {
		Map<Integer,Double> TFIDF_featurevector=new TreeMap<Integer,Double>();

		//First pass calculate total number of words.
		int NumOfTerms = 0;
		for (Map.Entry<Integer, Integer> e : indexed.entrySet()) 
			NumOfTerms+= e.getValue();

		//Second pass 
		for (Map.Entry<Integer, Integer> e : indexed.entrySet()) 
		{
			Integer WordID = e.getKey();
			double tfidf = e.getValue() / (double)NumOfTerms;
			TFIDF_featurevector.put(WordID , tfidf);
		}

		return TFIDF_featurevector;
	}


	/**
	 * index a phrase. 
	 *
	 * Returns: The indexed version of input.
	 */
	private Map<Integer, Integer> String2TF(String input) throws Exception {
		//Keeps record of the instances of each word in input String.
		Map<Integer, Integer> wordfreq = new TreeMap<Integer, Integer>(); 
		String tokens[] = input.split("\\s");		

		Integer freq;
		for (int t=0;t<tokens.length;t++) {							//For each word
			Integer w = _voc.getWordID(tokens[t]);
			if(w==null)		// Word not in Vocabulary--> Ignore it
				continue; 	

			freq = wordfreq.get(w);
			wordfreq.put(w, (freq == null) ? 1 : freq + 1);	//Increase number of instances of this word
		}

		return wordfreq;
	}

	/**
	 * Binary representation of Input string
	 * @param input
	 * @return
	 * @throws Exception
	 */
	private Map<Integer, Integer> String2Bin(String input) throws Exception {
		//Keeps record of the instances of each word in input String.
		Map<Integer, Integer> wordfreq = new TreeMap<Integer, Integer>(); 
		String tokens[] = input.split("\\s");		

		for (int t=0;t<tokens.length;t++) {							//For each word
			Integer w = _voc.getWordID(tokens[t]);
			if(w==null)		// Word not in Vocabulary--> Ignore it
				continue; 	

			wordfreq.put(w, 1);
		}

		return wordfreq;
	}
}
