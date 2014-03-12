/**

Word with ID 0 has a special meaning:
ID: 0
IDF: Number of documents processed so far to create IDFs

 */
package macsy.module.featuresExtractorTFIDF;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.TreeMap;


//READ ONLY
public class NGVoc   {

	private TreeMap<String, Integer> m_Word2ID = null;	//Word to ID
	private double[] m_ID2IDF = null;

	public int currentID = 0; //Number of words in Vocabulary (with the special one)
							// THe next number is the first available WordID
	
	private int m_NumOfDocuments = 0;

		
	private int MIN_DF_VALUE = 3;

	public NGVoc(String voc_filename)  throws Exception {
		LoadVocabulary(voc_filename);
	}

	/**
	 * Return the ID of the word or NULL if unknown word.
	 * @param word
	 * @return
	 */
	public Integer getWordID(String word) {
		return m_Word2ID.get(word);
	}

	/**
	 * Return the IDF of a word or 0 if there isn't such word 
	 * @param word
	 * @return
	 */
	public double getWordIDF(String word) throws Exception {
		Integer WordID = getWordID(word);
		
		if(WordID==null)
			return 0;
		
		return getWordIDF(WordID);
	}

	/**
	 * Assumes a correct  WordID < m_numOfWords
	 * @param word
	 * @return
	 */
	public Double getWordIDF(int WordID) throws Exception {
		return m_ID2IDF[WordID];
	}

	/**
	 * Warning. IT IS SLOW
	 * @param WordID
	 * @return
	 */
	public String getWord(int WordID)
	{
		if(m_Word2ID.containsValue(WordID))
		{
			Iterator<String> iter = m_Word2ID.keySet().iterator();
			
			while(iter.hasNext())
			{
				String key = iter.next() ;
				if(m_Word2ID.get( key ) == WordID)
					return key;
			}
			
		}
		return null;
	}
	
	
/**
 * Load a vocabulary and calculates IDFs
 * 
 * The vocabulary is formated:
 * 1st line Header
 * 2nd - N line WORD <Tab> TERM_FREQUENCY
 * 
 * 
 * 
 * @param voc_filename
 * @throws Exception
 */
	private void LoadVocabulary(String voc_filename) throws Exception 
	{
		File vocFile = new File(voc_filename);

		BufferedReader input = new BufferedReader( new FileReader(vocFile) );

		//LOAD HEADER
		String line = input.readLine();  //First line = HEADER OF VOC
		String[] tokens = line.split("\t");
		m_NumOfDocuments = Integer.parseInt(tokens[1]);
		currentID = Integer.parseInt(tokens[3]) + 1;

		//Initialize structures
		m_Word2ID = new TreeMap<String, Integer>();
		m_ID2IDF = new double[currentID];	//Skip 0 as WordID

		m_ID2IDF[0] = m_NumOfDocuments;
		
		int NextWordID = 1;
		double dfvalue = 0;
		//Start reading words
		while (( line = input.readLine()) != null)
		{
			tokens = line.split("\t");
			m_Word2ID.put(tokens[0], NextWordID);
			dfvalue = Double.parseDouble(tokens[1]);
			m_ID2IDF[NextWordID]  = (dfvalue<=MIN_DF_VALUE? 0 : Math.log( m_NumOfDocuments / (double)dfvalue));

		//	System.out.println("ID="+NextWordID+" idf="+m_ID2IDF[NextWordID]);
			
			NextWordID++;
		}
		input.close();
	}

}
