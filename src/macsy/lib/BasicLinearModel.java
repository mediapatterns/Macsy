
package macsy.lib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import macsy.module.featuresExtractorTFIDF.NGVoc;

/**
 * This class is the implementation of the interface named LinearModel
 *
 * @author Panagiota Antonakaki
 * Last Update: 30-05-2013
 *
 */

//READ ONLY
public class BasicLinearModel implements LinearModel
{
	private TreeMap<Integer, Double> m_Id2Value = null;	//ID to Value



	private int nextID ;	// The number is the first available WordID

	private boolean canWriteNewWords = true; // if we have the ability of writing new words in the voc

	private double magnitude = 0; // magnitude of w

	private String model_filename; // the name of the file with the model

	NGVoc Words;

        public BasicLinearModel() throws Exception {

        }
	/**
	 * Creates or loads a vocabulary with given name.
	 */
	public BasicLinearModel(String voc_filename) throws Exception
	{
		this.model_filename = voc_filename;

		//Initialize structures
		m_Id2Value = new TreeMap<Integer, Double>();

		nextID = 1;

		//If voc exists load it.
		loadModel(voc_filename);

		calculateMagnitude();

	}

	/*
	 * This function calculate the magnitude of the weights (in order to be able
	 * to normalize)
	 */
	private void calculateMagnitude()
	{
		for (Map.Entry<Integer, Double> e1 :getM_Id2Value().entrySet())
			magnitude += Math.pow(e1.getValue(), 2);

		if(magnitude<0)
			System.out.println("zero");

		magnitude = Math.sqrt( magnitude );
	}

	/*
	 * This function returns the magnitude of the weights
	 */
	public double getMagnitude()
	{
		return magnitude;
	}

	/**
	 * Return the ID or 0 if unknown word.
	 * @param word
	 * @return
	 */
	public int getID(String word)
	{
		Integer id = Integer.valueOf(word);
		return (!m_Id2Value.containsKey(id)?0:id.intValue());
	}

	/**
	 * The size of the vocabulary.
	 *
	 * @return the size
	 */
	public int getSize()
	{
		return getM_Id2Value().size();
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
		Double v = getM_Id2Value().get(id);
		return (v==null?0.0:v);
	}

	/**
	 * Sets the value of the word with "id"
	 * @param id
	 * @param value
	 */
	public void setValueByID(int id, double value)
	{
		if(id>=nextID || id<0)
			return;

		getM_Id2Value().put(id, value);
	}

	/**
	 * Sets the value of the word with "id" even if there is not in the voc
	 * @param id
	 * @param value
	 */
	public void setValueByNewID(int id, double value)
	{
		if(value == 0.0 || id == 0)
			return;

		getM_Id2Value().put(id, value);
	}

	/**
	 * Checks if word's id exists, if not it is added.
	 *
	 * @param word
	 * @return
	 * @throws Exception
	 */
	public synchronized void addWordByID(int wordID) throws Exception
	{
		if(canWriteNewWords==false)
			throw new Exception("FSimpleVoc locked. Unable to write new words.");

		Double value = getM_Id2Value().get(wordID);
		if(value==null)
		{
			getM_Id2Value().put(wordID, 1.0);
			nextID++;
		}
	}

	/**
	 * Saves / Updates the current vocabulary in disk.
	 * only m_Id2Value, no sparse, i.e. even when it's 0 it's printed
	 */
	public synchronized void saveVocAsIDToFeatures_NotSparse() throws Exception
	{
		System.out.print("Writing Vocabulary...");
		canWriteNewWords=false;

		StringBuilder s = new StringBuilder();

		s.append("Vocabulary Format: <id>\t<value>\n");

		System.out.println("voc size : " + getM_Id2Value().size());
		double value;

		for(int id = 0; id<179044; id++){
			value = getValueByID(id);
			s.append(id + "\t" + value + "\n");
		}

		BufferedWriter	fp = new BufferedWriter(new FileWriter( model_filename  ));
		fp.write(s.toString());
		fp.close();
		canWriteNewWords=true;
		System.out.println("DONE");
	}

	/**
	 * Calculates the Inner Product of the frequencies of the words in the text (TFs) and their
	 * weights (in the w_doc)
	 *
	 * @param w_doc the vocabulary with the weights
	 * @param x the input (map of IDs and TFs)
	 * @return the predicted output according to the input x
	 */
	public double calculateDotExtendedWX(DataPoint x) throws InterruptedException
	{
		double weight = 0.0;
		for(Map.Entry<Integer,Double> e : x.getFeaturesMap().entrySet())
			weight += getValueByID( e.getKey() ) * e.getValue();
		weight += getB();

		return weight;
	}

	/**
	 * Calculates the Inner Product (without bias) of the frequencies of the words
	 * in the text (TFs) and their weights (in the w_doc)
	 *
	 * @param w_doc the vocabulary with the weights
	 * @param x the input (map of IDs and TFs)
	 * @return the predicted output according to the input x
	 */
	public double calculateDotWX(DataPoint x) throws InterruptedException
	{
		double weight = 0.0;
		for(Map.Entry<Integer,Double> e : x.getFeaturesMap().entrySet())
			weight += getValueByID( e.getKey() ) * e.getValue();

		return weight;
	}

	@Override
	public Map<String,Double> wordCloudGetTopFeatures(int n)
	{
		Map<String,Double> words_ids = new HashMap<String,Double>();

		List<Integer> WordsCloud = topFeatures(n);
		for(Integer e : WordsCloud)
			words_ids.put(Words.getWord(e), getWi(e));

		return words_ids;
	}

	public List<Integer> topFeatures (int n){
		Map<Integer, Double> TopWords_Id2Words =
				new TreeMap<Integer, Double>();
		Integer maxKey = getM_Id2Value().firstKey();

		List<Integer> sortedIDs = new LinkedList<Integer>();

		for (int i = 0; i < n; i++)
		{
			boolean found = true;
			Iterator<Entry<Integer, Double>> iterator =
					getM_Id2Value().entrySet().iterator();
			while (iterator.hasNext() && found){
				Map.Entry<Integer, Double> mapEntry =
						(Entry<Integer, Double>) iterator.next();
				if(mapEntry.getValue() != Double.POSITIVE_INFINITY &&
						mapEntry.getKey() != 0 )
				{
					maxKey = mapEntry.getKey();
					found = Sequential_Search(maxKey, TopWords_Id2Words);
				}
			}

			double maxValue = getM_Id2Value().get(maxKey);

			for(Map.Entry<Integer, Double> e : getM_Id2Value().entrySet())
			{
				if(e.getValue() > maxValue &&
						!Sequential_Search(e.getKey(),TopWords_Id2Words))
				{
					maxValue = e.getValue();
					maxKey = e.getKey();
				}
			}
			TopWords_Id2Words.put(maxKey, maxValue);
			sortedIDs.add( maxKey );
		}


		return sortedIDs;
	}

	@Override
	public Map<String,Double> wordCloudGetLeastFeatures(int n)
	{
		Map<String,Double> words_ids = new HashMap<String,Double>();

		List<Integer> WordsCloud = leastFeatures(n);
		for(Integer e : WordsCloud)
			words_ids.put(Words.getWord(e), getWi(e));

		return words_ids;
	}

	public  List<Integer>  leastFeatures (int n){
		TreeMap<Integer, Double> LeastWords_Id2Words =
				new TreeMap<Integer, Double>();

		List<Integer> sortedIDs = new LinkedList<Integer>();


		Integer minKey = getM_Id2Value().firstKey();

		for (int i = 0; i < n; i++)
		{
			boolean found = true;
			Iterator<Entry<Integer, Double>> iterator =
					getM_Id2Value().entrySet().iterator();
			while (iterator.hasNext() && found){
				Map.Entry<Integer, Double> mapEntry =
						(Entry<Integer, Double>) iterator.next();
				if(mapEntry.getValue() != Double.POSITIVE_INFINITY &&
						mapEntry.getKey() != 0 )
				{
					minKey = mapEntry.getKey();
					found = Sequential_Search(minKey, LeastWords_Id2Words);
				}
			}

			double minValue = getM_Id2Value().get(minKey);

			for(Map.Entry<Integer, Double> e : getM_Id2Value().entrySet())
			{
				if(e.getValue() < minValue &&
						!Sequential_Search(e.getKey(),LeastWords_Id2Words))
				{
					minValue = e.getValue();
					minKey = e.getKey();
				}
			}
			LeastWords_Id2Words.put(minKey, minValue);
			sortedIDs.add( minKey );
		}

		return sortedIDs;
	}

	public boolean Sequential_Search(Integer target,
			Map<Integer, Double> mapToSearch)
	{
		boolean found = false;

		for(Map.Entry<Integer, Double> e : mapToSearch.entrySet())
			if(e.getKey() == target)
				found = true;

		return found;
	}

	/**
	 * Modifies vocabulary as:
	 *
	 * W  = W / ||W||
	 *
	 * where ||W|| is the second norm of W.
	 *
	 */
	public void normalize(){
		calculateMagnitude();
		if(getMagnitude() == 0)
			return; // Nothing to normalize

		for (Map.Entry<Integer, Double> e1 : getM_Id2Value().entrySet())
			getM_Id2Value().put( e1.getKey() , e1.getValue() / getMagnitude());

		magnitude = 1.0;
	}


	/**
	 * Updates the model
	 *
	 * w(t+1) = w(t) + eta(d(t) - y_hat)x
	 *			where 	d is the desired output (-1 or 1)
	 *					y_hat the predicted value
	 *					x the input
	 * The input is already multiplied by the eta(d(t) - y_hat), so here
	 * only the previous value of the weight is added.
	 * It also updates the bias by value (b(t+1) = b(t) + value)
	 */

	@Override
	public void addToW(DataPoint x, double value) {
		for(Map.Entry<Integer,Double> e : x.getFeaturesMap().entrySet())
		{
			Double w_prev = getM_Id2Value().get(  e.getKey()  );
			if (w_prev != null)
				try {
					setValueByID( e.getKey(), w_prev + e.getValue() );
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			else
				try {
					setValueByNewID( e.getKey(), e.getValue() );
				} catch (Exception e1) {
					e1.printStackTrace();
				}
		}


		addToB(value);
	}


       /* This function updates the pocket weight vector using the commity concept*/

      


	@Override
	public void addToB(double c) {
		//Constant
		double w_prev = getM_Id2Value().get( 0 );
		try {
			setValueByID(0, w_prev + c);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	@Override
	public void reset() {
		m_Id2Value = new TreeMap<Integer, Double>();
		nextID = 1;

		setB(0.0);

	}



	@Override
	public DataPoint getW() {
		return new DataPoint(getM_Id2Value());
	}



	@Override
	public void setW(DataPoint w) {
		getM_Id2Value().clear();
		for(Map.Entry<Integer, Double> e : w.getFeaturesMap().entrySet())
			getM_Id2Value().put(e.getKey(), e.getValue());
	}



	@Override
	public double getB() {
		//Constant
                
		return getM_Id2Value().get( 0 );
	}



	@Override
	public void setB(double b) {
		getM_Id2Value().put(0, b);

	}



	@Override
	public double predict(DataPoint x) {
		try {
			return calculateDotExtendedWX(x);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public double score(DataPoint x) {
		try {
			return calculateDotWX(x);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return 0;
		}
	}



	@Override
	public void saveModel(String fileName) {
		System.out.print("Writing Vocabulary...");
		canWriteNewWords=false;

		StringBuilder s = new StringBuilder();

		s.append("Vocabulary Format: <id>\t<value>\n");
		Double value = null;
		Integer id = null;

		System.out.println("voc size : " + getM_Id2Value().size());

		id = 0;
		value = getB();

		s.append(id +"\t"+value+"\n");

		for(Map.Entry<Integer, Double> e : getM_Id2Value().entrySet() )
		{
			try
			{
				id = e.getKey();
				value = e.getValue();
			}
			catch(Exception ex)
			{
				System.out.println("Error writing voc: "+ ex.toString());
				continue;
			}

			if((id!=null) && (value!=null) && (value!=0))
				s.append(id +"\t"+value+"\n");
		}
		BufferedWriter fp;
		try {
			fp = new BufferedWriter(new FileWriter( fileName  ));
			fp.write(s.toString());
			fp.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		canWriteNewWords=true;
		System.out.println("DONE");

	}


        public void savePocketModel(String fileName) {
		System.out.print("Writing Vocabulary...");
		canWriteNewWords=false;

		StringBuilder s = new StringBuilder();

		s.append("Vocabulary Format: <id>\t<value>\n");
		Double value = null;
		Integer id = null;

		System.out.println("voc size : " + getM_Id2Value().size());

		id = 0;
		value = getB();

		s.append(id +"\t"+value+"\n");

		for(Map.Entry<Integer, Double> e : getM_Id2Value().entrySet() )
		{
			try
			{
				id = e.getKey();
				value = e.getValue();
			}
			catch(Exception ex)
			{
				System.out.println("Error writing voc: "+ ex.toString());
				continue;
			}

			if((id!=null) && (value!=null) && (value!=0))
				s.append(id +"\t"+value+"\n");
		}
		BufferedWriter fp;
		try {
			fp = new BufferedWriter(new FileWriter( model_filename  ));
			fp.write(s.toString());
			fp.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		canWriteNewWords=true;
		System.out.println("DONE");

	}


	@Override
	public void loadModel(String fileName){
		File vocFile = new File(fileName);

		//No voc return.
		if(!vocFile.exists()){
			setB(0.0);
			return;
		}

		//LOAD HEADER
		String line;
		BufferedReader input;
		try {
			input = new BufferedReader( new FileReader(fileName) );
			//First line = HEADER OF VOC //Ignore
			line = input.readLine();

			//Start reading words
			while (( line = input.readLine()) != null)
			{
				String toks[] = line.split("\t");
				int id = Integer.parseInt(toks[0]);
				double value = Double.parseDouble(toks[1]);
				if(id == 0)
					setB(value);
				else
					getM_Id2Value().put(id, value);

			}
			input.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		catch (IOException e2) {
			e2.printStackTrace();
		}
		catch(Exception e3)
		{
			e3.printStackTrace();
		}
	}

	@Override
	public double getWi(int featureID) {
		return getM_Id2Value().get(featureID);
	}

	@Override
	public void wordCloudSetVocabulary(String voc_filename) throws Exception{
		Words = new NGVoc(voc_filename);
	}

    /**
     * @return the m_Id2Value
     */
    public TreeMap<Integer, Double> getM_Id2Value() {
        return m_Id2Value;
    }

    public void Update_PocketWeights(DataPoint sample, TreeMap prev_w) {
        throw new UnsupportedOperationException("Not supported yet.");
    }



}
