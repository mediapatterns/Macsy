package macsy.lib;

import java.util.Map;
import java.util.TreeMap;


/**
 * Linear Model of type wx+b.
 * 
 * @author csxif
 *
 */
public interface LinearModel {

	/**
	 * Adds x to w.
	 * 
	 * @param x
	 */
	void addToW(DataPoint x, double value);

	/**
	 * Adds c to b. 
	 * @param b
	 */
	void addToB(double c);


	/**
	 * Sets w=0 and B=0.
	 */
	void reset();
	
	/**
	 * Returns W.	
	 * @return
	 */
	DataPoint getW();
	
	/**
	 * Returns the value of feature with given ID. 
	 * @param featureID
	 * @return
	 */
	double getWi(int featureID);
	

	/**
	 * Sets W.
	 * @param w
	 */
	void setW(DataPoint w);

	/**
	 * Returns B.
	 * @return
	 */
	double getB();
	
	/**
	 * Sets B.
	 * @param b
	 */
	void setB(double b);

	/**
	 * Returns y = wx+b (actual output)
	 * 
	 * @param x
	 * @return
	 */
	double predict(DataPoint x);
	
	/**
	 * Returns y = wx (score)
	 * 
	 * @param x
	 * @return
	 */
	double score(DataPoint x);

	/**
	 * Saves the model to file.
	 * 
	 * @param fileName
	 */
	void saveModel(String fileName);

        void savePocketModel(String fileName);

	/**
	 * Load model from file.
	 * @param fileName
	 */
	void loadModel(String fileName);

        void Update_PocketWeights(DataPoint sample, TreeMap prev_w);

        void normalize();
	/**
	 * Returns a map of the top-n features with the highest value and the
	 * corresponding words of the vocabulary. 
	 * @param number
	 * @return
	 */
	Map<String,Double> wordCloudGetTopFeatures (int n);
	
	/**
	 * Returns a map of the least-n features with the lowest value and the
	 * corresponding words of the vocabulary. 
	 * @param number
	 * @return
	 */
	Map<String,Double> wordCloudGetLeastFeatures (int n);
	 
	 /**
	  * Sets the vocabulary of the language used from a filename 
	  * @param vocabulary: the filename of the vocabulary used
	  */
	 void wordCloudSetVocabulary(String voc_filename) throws Exception;
	 
	 TreeMap<Integer, Double> getM_Id2Value() throws Exception;
	 
}
