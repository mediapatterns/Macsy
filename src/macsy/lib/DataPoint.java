/**
 * DataPoint have a dual representation of their features.
 * 
 * The first is a TreeMap
 * The second is a String vector of format: <feature1>:<value1> <feature2>:<value2> ...
 * 
 */
package macsy.lib;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
* @author      Ilias Flaounas <iliasfl@gmail.com>
* @version     1.0                   
* @since       2012-11-01
* 
*/
@SuppressWarnings("unchecked")
public class DataPoint extends Object implements Comparable {
	private int ID;	// Unique ID to identify this DataPoint from other datapoints.
	private int RealLabel = UNKNOWN_LABEL; // Used for classification := Ground Truth
	private int PredictedLabel = UNKNOWN_LABEL;  // Used for classification := Predicted Label
	private Double PredictedLabel_Value = null;  // Used for classification := Predicted Label

	public static final int UNKNOWN_LABEL = Integer.MIN_VALUE;

	//	Map<Integer,Integer> indexed;

	//Two representations of the same data. 
	//Map is used for computations, and string for outputing features.
	private Map<Integer,Double> FeaturesMap_ = null;
	private String FeaturesString;	// has no label in it

	/**
	 * 	DO NOT USE IT DIRECTLY ONLY THREW GetMagnitude function, that calculates it
	 */ 
	private Double m_Magnitude =  null; 

	//	public double decision_value;
	public byte[] md5=null;

	/**
	 * Two datapoints are the same if their features are the same
	 * It doesn't matter what labels they have
	 */
	public int  compareTo(Object o) {
		return FeaturesString.compareTo(((DataPoint)o).FeaturesString);
	}

	/**
	 * Creates a clone of current object
	 */
	public DataPoint clone()
	{
		DataPoint cp = new DataPoint( FeaturesString );
		cp.setID( ID );
		cp.setRealLabel(RealLabel);
		cp.setPredictedLabel( PredictedLabel);
		cp.setPredictedLabel_Value(PredictedLabel_Value);
		return cp;
	}
	
	/**
	 * Create a new Point by Map.
	 * String representation is built automatically.   
	 * Label is set to UNKNOWN
	 * 
	 * @param features_map
	 */
	public DataPoint(Map<Integer,Double> features_map) {
		FeaturesMap_ = features_map;
		setRealLabel(UNKNOWN_LABEL);
		Features_Map2String();
	}

	/**
	 * The input feature string has NO label. 
	 * It is set in here to UNKNOWN_LABEL.
	 *  
	 * @param features <feature1>:<value1> <feature2>:<value2> ...
	 * @param l_label
	 */
	public DataPoint(String features) {
		//features = features_map;
		setRealLabel(UNKNOWN_LABEL);

		FeaturesString =  features;
	//	Features_String2Map();
	}



	/**
	 * Create a new Point by Map and assign label.
	 * String representation is built automatically.   
	 * @param features_map
	 */
	public DataPoint(Map<Integer,Double> features_map,int l_label) {
		FeaturesMap_ = features_map;
		setRealLabel(l_label);
		Features_Map2String();
	}

	/**
	 * features_list is a list of pairs like <feature_ID, value>
	 * @param features_list
	 */
	public DataPoint(List<Double> features_list)
	{
		FeaturesMap_ = new TreeMap<Integer,Double>();
		int index;
		double value;
		for(int f=0; f<features_list.size(); f+=2)
		{
			index = (int) Math.round(features_list.get(f));
			value = features_list.get(f+1);
			FeaturesMap_.put(index, value);
		}
		
		setRealLabel(UNKNOWN_LABEL);
		Features_Map2String();
	}
	
	/**
	 *  Create a new Point by Map and assign label.
	 *  Map representation is built automatically.   
	 * @param features <feature1>:<value1> <feature2>:<value2> ...
	 * @param l_label
	 */
	public DataPoint(String features,int l_label) {
		setRealLabel(l_label);
		FeaturesString =  features;
		//Features_String2Map();
	}

	/**
	 *  Create a new Point by Map and assign label.
	 *  Map representation is built automatically.   
	 * @param features <feature1>:<value1> <feature2>:<value2> ...
	 * @param l_label
	 */
	public DataPoint(String features,int _label,int _ID) {
		this(features,_label);
		ID = _ID;
	}



	/**
	 * Uses features_string as input to produce the features Map
	 *
	 */
	private void Features_String2Map()
	{
		FeaturesMap_ = new TreeMap<Integer,Double>();

		StringTokenizer st1 = new StringTokenizer(FeaturesString," :");

		while(st1.hasMoreTokens())
		{
			Integer ind= new Integer(st1.nextToken());
			Double freq= new Double(st1.nextToken());
			FeaturesMap_.put(ind, freq);
		}
	}



	/**
	 * Uses features map as input to produce the features String
	 *
	 */
	private void Features_Map2String() 
	{
		if(FeaturesMap_==null)
		{
			System.out.print("Can not create features string, since Map is empty");
			return;
		}
		
		
		StringBuffer featurevector=new StringBuffer(1000);
		int term = 0;
		double value = 0 ;

		//Find and append classlabel first

		DecimalFormat threePlaces = new DecimalFormat("0.000");

		for (Object e : FeaturesMap_.entrySet()) {
			term = 	((Map.Entry<Integer, Double>)e).getKey();
			value= 	((Map.Entry<Integer, Double>)e).getValue();

			featurevector.append(term + ":"+ threePlaces.format(value)+" ");
		}

		FeaturesString = featurevector.toString();
	}

	/**
	 * Returns the Magnitude of vector x  (2-norm)
	 * 
	 * ||x|| = sqrt(x1*x1 + x2*x2 + ...xn*xn)
	 * 
	 * It also stores the value of the magnitude, and return that value for future usage.
	 * 
	 */
	public double getMagnitude()
	{
		if(FeaturesMap_==null)
			Features_String2Map();
			
		if(m_Magnitude!=null)
			return m_Magnitude;

		m_Magnitude = new Double(0);

		for (Map.Entry<Integer, Double> e1 : FeaturesMap_.entrySet()) 
			m_Magnitude += Math.pow(e1.getValue(), 2);

		m_Magnitude = Math.sqrt( m_Magnitude );

		return m_Magnitude;
	}



	/**
	 * Return the Squared Eucledian distance to the given point.
	 * 
	 * @param x
	 * @return
	 */
	public  double getSquaredEuclideanDistance(DataPoint x)
	{
		double d = 0;

		if(x.FeaturesMap_==null)
			x.Features_String2Map();
		
		if(FeaturesMap_==null)
			Features_String2Map();
		
		
		//First parse get common values or values in x
		for (Object e : x.FeaturesMap_.entrySet()) {
			int x_key = 	((Map.Entry<Integer, Double>)e).getKey();
			double x_value = 	((Map.Entry<Integer, Double>)e).getValue();

			Double y_value = FeaturesMap_.get(x_key);
			if(y_value==null)
				y_value = new Double(0);

			d += Math.pow(x_value - y_value , 2.0) ;
		}

		/// second parse get values in local point that do not exist in x
		for (Object e : FeaturesMap_.entrySet()) {
			int y_key = 	((Map.Entry<Integer, Double>)e).getKey();

			if(!x.FeaturesMap_.containsKey(y_key))
			{
				double y_value = 	((Map.Entry<Integer, Double>)e).getValue();
				d += Math.pow( y_value , 2.0) ;
			}
		}

		return d ;
	}

	/**
	 * Return the Eucledian distance to the given point.
	 * 
	 * @param x
	 * @return
	 */
	public  double getEuclideanDistance(DataPoint x)
	{
		if(x.FeaturesMap_==null)
			x.Features_String2Map();
		
		if(FeaturesMap_==null)
			Features_String2Map();
		
		double d = 0;

		//First parse get common values or values in x
		for (Object e : x.FeaturesMap_.entrySet()) {
			int x_key = 	((Map.Entry<Integer, Double>)e).getKey();
			double x_value = 	((Map.Entry<Integer, Double>)e).getValue();

			Double y_value = FeaturesMap_.get(x_key);
			if(y_value==null)
				y_value = new Double(0);

			d += Math.pow(x_value - y_value , 2.0) ;
		}

		/// second parse get values in local point that do not exist in x
		for (Object e : FeaturesMap_.entrySet()) {
			int y_key = 	((Map.Entry<Integer, Double>)e).getKey();

			if(!x.FeaturesMap_.containsKey(y_key))
			{
				double y_value = 	((Map.Entry<Integer, Double>)e).getValue();
				d += Math.pow( y_value , 2.0) ;
			}
		}

		return Math.sqrt( d );
	}

	/**
	 * Return the Chebyshev distance to the given point.
	 * 
	 * Chebyshev = L(inf)  = max( | xi - yi | )
	 * 
	 * @param x
	 * @return
	 */
	public  double getChebyshevDistance(DataPoint x)
	{
		if(x.FeaturesMap_==null)
			x.Features_String2Map();
		if(FeaturesMap_==null)
			Features_String2Map();
		
		double d = 0;

		double local_d = 0;
		//First parse get common values or values in x
		for (Map.Entry<Integer, Double> e : x.FeaturesMap_.entrySet()) {
			int x_key = 	e.getKey();
			double x_value = 	e.getValue();

			Double y_value = FeaturesMap_.get(x_key);
			if(y_value==null)
				y_value = new Double(0);

			local_d = Math.abs( y_value - x_value );
			if(local_d > d )
				d = local_d;
		}

		/// second parse get values in local point that do not exist in x
		for (Map.Entry<Integer, Double> e : FeaturesMap_.entrySet()) {
			int y_key =  e.getKey();

			if(!x.FeaturesMap_.containsKey(y_key))
			{
				local_d =  Math.abs( e.getValue() );
				if(local_d > d )
					d = local_d;
			}
		}

		return d;
	}

	/**
	 * Computes L1 norm 
	 * 
	 * ||X - Y||  = sum( |xi-yi| )
	 * 
	 * @param x
	 * @return
	 */
	public  double getManhattanDistance(DataPoint x)
	{
		if(x.FeaturesMap_==null)
			x.Features_String2Map();
		
		if(FeaturesMap_==null)
			Features_String2Map();
		
		double d = 0;

		//First parse get common values or values in x
		for (Map.Entry<Integer, Double> e : x.FeaturesMap_.entrySet()) {
			int x_key = 	e.getKey();
			double x_value = 	e.getValue();

			Double y_value = FeaturesMap_.get(x_key);
			if(y_value==null)
				y_value = new Double(0);

			d += Math.abs( y_value - x_value );
		}

		/// second parse get values in local point that do not exist in x
		for (Map.Entry<Integer, Double> e : FeaturesMap_.entrySet()) {
			int y_key =  e.getKey();

			if(!x.FeaturesMap_.containsKey(y_key))
			{
				d +=  Math.abs( e.getValue() );
			}
		}

		return d;
	}

	/**
	 * Return the dot product of point with x
	 * 
	 * x.y = x1*y1 + x2*y2 + ... + xn*yn
	 * 
	 * @param x
	 * @return
	 */
	public double getDotProduct(DataPoint x) {
		double dotproduct = 0;

		for (Map.Entry<Integer, Double> e1 : x.FeaturesMap_.entrySet()) 
			if(FeaturesMap_.containsKey(e1.getKey())) //Local Point
				dotproduct+=e1.getValue() * FeaturesMap_.get(e1.getKey());

		return dotproduct;
	}


	/**
	 * Normalized Cosine distance between two vectors.
	 * @param a1
	 * @param a2
	 * @return
	 */
	public double getCosineSimilarity(DataPoint x) {
		double x_magnitude = x.getMagnitude();
		double y_magnitude = getMagnitude();

		if(x_magnitude==0)
			return 0;
		if(y_magnitude==0)
			return 0;

		double dotproduct = getDotProduct(x);

		return dotproduct / ( x_magnitude * y_magnitude );
	}

	/**
	 * SOS. Modifies X as:
	 * 
	 * X  = X / ||X||
	 * 
	 * where ||X|| is the second norm of X.
	 *
	 */
	public void normalize(){
		getMagnitude();
		if(m_Magnitude == 0)
			return; // Nothing to normalize

		for (Map.Entry<Integer, Double> e1 : FeaturesMap_.entrySet())
			FeaturesMap_.put( e1.getKey() , e1.getValue() / m_Magnitude);

		Features_Map2String();

		m_Magnitude = 1.0;
	}


	public void setRealLabel(int realLabel) {
		RealLabel = realLabel;
	}

	public int getRealLabel() {
		return RealLabel;
	}

	public void setPredictedLabel(int predictedLabel) {
		PredictedLabel = predictedLabel;
	}

	public int getPredictedLabel() {
		return PredictedLabel;
	}
	
	public void setPredictedLabel_Value(Double predictedLabel_Value) {
		PredictedLabel_Value = predictedLabel_Value;
	}

	public Double getPredictedLabel_Value() {
		return PredictedLabel_Value;
	}
	
	public void setID(int iD) {
		ID = iD;
	}

	public int getID() {
		return ID;
	}


	/**
	 * Get Features Map
	 * used inside package in DataSet class
	 */
	Map<Integer,Double> getFeaturesMap() 
	{
		if(FeaturesMap_==null)
			Features_String2Map();
		
		return FeaturesMap_;
	}

	public String getFeatures() 
	{
		return FeaturesString;
	}

	public void setFeatures(String newfeatures) 
	{
		FeaturesString =  newfeatures;
		Features_String2Map();
	}
	
	public Double getFeatureValue(int featureID)
	{
		if(FeaturesMap_==null)
			Features_String2Map();
		return FeaturesMap_.get(featureID);
	}
	

}
