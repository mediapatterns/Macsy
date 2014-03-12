/**
 * Preprocessing 
 * 
 * Used before indexing to:
 *  Remove Stop words
 *  Stem
 *  Remove non-english chars
 *  
 *   Author Ilias Flaounas
 */
package macsy.module.featuresExtractorTFIDF;


import java.io.IOException;
//import java.util.ArrayList;
import java.io.*;
import java.util.TreeSet;


public class NGPreprocessing {

	private final int MAX_WORD_LENGTH = 50;
	private int MIN_WORD_LENGTH = 3;
	

	
	private TreeSet<String> m_StopWords;

	private Stemmer m_stemmer;	//Stemmer object


	public NGPreprocessing(String stopwords_filename) {
		m_stemmer = new Stemmer();	//stemmer;
		//	System.out.print("Loading stop words...");
		m_StopWords = new TreeSet<String>();
		if(stopwords_filename!=null)
			LoadStopWords(stopwords_filename);
	}
	
	public NGPreprocessing(String stopwords_filename, int minWorldLength) {
		this(stopwords_filename);
		MIN_WORD_LENGTH = minWorldLength;
	}


	/**
	 * Uses default stop words file at resources/stopwords.txt
	 */
	public NGPreprocessing() {
		this("resources/stopwords.txt");
	}

	
	private boolean IsAcceptedLetter(char c) {
		if (((c >= 'a') && (c <= 'z')))  return true; // a - z
		if (((c >= 'A') && (c <= 'Z')) ) return true; // A - Z
		//if (((c >= '0') && (c <= '9')))  return true; // 0 - 9
		return false;
	}


	/*
	 * Uses Porter stemmer and stop world removal. 
	 */
	public String doPreprocess(String inp) {
		String input =  inp+" ";
		char[] w  =  new char[MAX_WORD_LENGTH];	//MAX wordlength
		int input_size = input.length();
		char[] buf_in = input.toCharArray();

		StringBuffer buf_out = new StringBuffer(1000);

		try {

			int k=0; //word pointer;
			for(int i=0;i<input_size;i++)	//Char in input pointer
			{
				if ( IsAcceptedLetter(buf_in[i]) )
				{
					if(k < MAX_WORD_LENGTH -1 )	//Cut word if too large
					{
						w[k++]=Character.toLowerCase(buf_in[i]);
					}
				}
				else 
				{
					if (k>=MIN_WORD_LENGTH)	//MIN LENGTH OF A WORD 
					{
						m_stemmer.add(w, k);
						m_stemmer.stem();
						//REMOVE IF IS A STOP WORD!!!
						if(!m_StopWords.contains(m_stemmer.toString()))
						{
							//	System.out.print( s.toString()+" " );
							buf_out.append(m_stemmer.toString()+" ");
						}

						//	dic.add(s.toString());
					}
					k=0;
				}

			}
		}
		catch(Exception e) {
			//System.out.printf("Exception in Preprocessing. Maybe too big word (>100)!. "+e.toString());
		}
		return buf_out.toString().trim();

	}

	private void LoadStopWords(String stop_words_filename) 
	{
		try {
			File stopwordsFile = new File(stop_words_filename);
			
			BufferedReader input = new BufferedReader( new FileReader(stopwordsFile) );
			String line = null; //not declared within while loop
			Stemmer s = new Stemmer();	//Stem words list!

			while (( line = input.readLine()) != null){
				s.add(line.toCharArray(), line.length());
				s.stem();
				m_StopWords.add(s.toString());
				//	System.out.println(line +"->"+s.toString());
			}
			//	System.out.println("DONE");
		}
		catch (FileNotFoundException ex) {
			System.err.println("Stop words file not found!");
		}
		catch(IOException ex) {
			System.err.println("Error reading words file!");
		}
	}

}
