/**

Word with ID 0 has a special meaning:
ID: 0 ==> Word not in Vocabulary

Words are saved in order, by id: 
firstword\tValue =>id=1
secondword\tValue =>id=2
...


 */
package macsy.lib;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;


//READ ONLY
public class VocabularyOnWords   
{
	private TreeMap<String, Integer> m_Word2ID 	= null;	//Word to ID
	private TreeMap<Integer, Double> m_Id2Value = null;	//ID to Value

	private int nextID ;	// The number is the first available WordID

	private boolean canWriteNewWords = true;

	/**
	 * magnitude of w
	 */
	private double magnitude = 0;


	private String voc_filename;

	/**
	 * Creates or loads a vocabulary with given name.
	 */
	public VocabularyOnWords(String voc_filename) throws Exception 
	{
		this.voc_filename = voc_filename;

		//Initialize structures
		m_Word2ID = new TreeMap<String, Integer>();
		m_Id2Value = new TreeMap<Integer, Double>();
		nextID = 1;

		m_Id2Value.put(0, 0.0);
		m_Word2ID.put("#CONSTANT#", 0);

		//If voc exists load it.
		loadVocabulary(voc_filename);

		calculateMagnitude();
	}
	
	public void resetVocabulary()
	{
		m_Word2ID = new TreeMap<String, Integer>();
		m_Id2Value = new TreeMap<Integer, Double>();
		nextID = 1;

		m_Id2Value.put(0, 0.0);
		m_Word2ID.put("#CONSTANT#", 0);
	}

	private void calculateMagnitude()
	{
		for (Map.Entry<Integer, Double> e1 :m_Id2Value.entrySet()) 
			magnitude += Math.pow(e1.getValue(), 2);

		magnitude = Math.sqrt( magnitude );
	}

	public double getMagnitude()
	{
		return magnitude;
	}


	/**
	 * Return the ID of the word or 0 if unknown word.
	 * @param word
	 * @return
	 */
	public int getIDofWord(String word) 
	{
		Integer id = m_Word2ID.get(word);
		return (id==null?0:id.intValue());
	}


	/**
	 * The value of the word.
	 * 0, if word not found.
	 * 
	 * @param id
	 * @return
	 */
	public double getValueByID(int id) 
	{
		Double v = m_Id2Value.get(id); 
		return (v==null?0:v);
	}

	/**
	 * Sets the value of the word with "id"
	 * @param id
	 * @param value
	 * @throws Exception 
	 */
	public void setValueByID(int id, double value) throws Exception 
	{
		if(id>=nextID || id<0)
		{
			return;
			//throw new Exception("Unknown word");
		}

		m_Id2Value.put(id, value);
	}

	/**
	 * Returns the ID of the word.
	 * Warning: Linear time to size of vocabulary.
	 * 
	 * Returns null if wordID not found.
	 * 
	 * @param wordID
	 * @return
	 */
	public String getWordByID(int wordID)
	{
		Iterator<String> iter = m_Word2ID.keySet().iterator();

		while(iter.hasNext())
		{
			String key = iter.next() ;
			if(m_Word2ID.get( key ).equals(wordID))
				return key;
		}
		return null;
	}

	/**
	 * Checks if words exists, if not it is added.
	 * The id of the word is returned.
	 * 
	 * @param word
	 * @return
	 * @throws Exception 
	 */
	public synchronized int addWord(String word) throws Exception
	{
		if(canWriteNewWords==false)
			throw new Exception("FSimpleVoc locked. Unable to write new words.");

		int id = getIDofWord(word);
		if(id==0)
		{
			id = nextID;
			m_Word2ID.put(word, id);
			m_Id2Value.put(id, 0.0);
			nextID++;
		}

		return id;
	}

	/**
	 * Saves / Updates the current vocabulary in disk.
	 * m_Word2ID
	 * m_Id2Value
	 */
	public synchronized void saveVocabulary() throws Exception
	{
		System.out.print("Writing Vocabulary...");
		canWriteNewWords=false;

		StringBuilder s = new StringBuilder();
			
		s.append("Vocabulary Format: <word>\t<value>\n");
		Double value = null;
		String word = null;
		Integer id = null;
		
		for(Map.Entry<String,Integer> e : m_Word2ID.entrySet() )
		{
			try
			{
				word = e.getKey();
				id = e.getValue();
				value = m_Id2Value.get(id);
			}
			catch(Exception ex)
			{
				System.out.println("Error writing voc: "+ ex.toString());
				continue;
			}

			if((word!=null) && (value!=null) && (value!=0))
				s.append(word +"\t"+value+"\n");
		}
		BufferedWriter	fp = new BufferedWriter(new FileWriter( voc_filename  ));
		fp.write(s.toString());
		fp.close();
		canWriteNewWords=true;
		System.out.println("DONE");
	}

	/**
	 * N^2 complexity
	 * 
	 * @return
	 */
	public String exportVoc()
	{
		StringBuffer report = new StringBuffer();

		for(Map.Entry<Integer, Double> e : m_Id2Value.entrySet())
		{
			int id = e.getKey();
			String word = getWordByID(id);
			report.append( word+"\t"+e.getValue()+"\n"  );
		}

		return report.toString();
	}




	/**
	 * Load a vocabulary and calculates IDFs
	 * 
	 * The vocabulary is formated:
	 * 1st line		: Number Of Words
	 * 2nd - N line	: WORD\n
	 * 
	 * 
	 * 
	 * @param voc_filename
	 * @throws Exception
	 */
	private void loadVocabulary(String voc_filename) throws Exception 
	{
		File vocFile = new File(voc_filename);

		//No voc return.
		if(!vocFile.exists())
			return;

		BufferedReader input = new BufferedReader( new FileReader(vocFile) );

		//LOAD HEADER
		String line = input.readLine();  //First line = HEADER OF VOC //Ignore

		//Start reading words
		while (( line = input.readLine()) != null)
		{
			try
			{
				String toks[] = line.split("\t");
				int id= addWord(toks[0]);
				double value = Double.parseDouble(toks[1]);
				setValueByID(id,value);
			}
			catch(Exception e)
			{
			}
		}

		input.close();
	}

	/**
	 * Sets the canWriteNewWords flag. If false then no new words are written and exception is thrown.
	 * @param canWriteNewWords
	 */
//	public void setCanWriteNewWords(boolean canWriteNewWords)
//	{
//		this.canWriteNewWords = canWriteNewWords;
//	}

}
