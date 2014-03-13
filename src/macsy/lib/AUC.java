package macsy.lib;

/**
 * This class is used to calculate the value for the Area Under the Curve.
 *   
 * @author Panagiota Antonakaki
 * Last Update: 30-05-2013 
 */

public class AUC {						
	double last_pos_score; //w*x +b  (last predicted score if real label was positive)
	double last_neg_score; //w*x +b (last predicted score if real label was negative)
	
	private double 	expMovingAverage_AUC;	//AUC
	private double 	expMovingAverage_a;		// lambda
	
	public AUC(){
		last_pos_score = 0.0;				
		last_neg_score = 0.0;
		expMovingAverage_AUC = 1; // initial value for auc (1 or .5)
	}
	
	/**
	 * Calculates the value of AUC according to the exponential moving average formula
	 * according to the boolean value of the input
	 * @param A: is the boolean value according to which the AUC will be updated
	 */
	public void expAUC_addPoint( boolean A ){
		if(A)
			expMovingAverage_AUC = expMovingAverage_a + (1.0 - expMovingAverage_a)*expMovingAverage_AUC;
		else
			expMovingAverage_AUC = (1.0 - expMovingAverage_a)*expMovingAverage_AUC;		
		
	}
	
	/**
	 * Returns the value of AUC
	 * @return the exponential moving average of AUC
	 */
	public double expMovAvGetAUC(){
		return this.expMovingAverage_AUC;
	}
	
	/**
	 * Calculates the value of alpha used in the exponential moving average according
	 * to the window : alpha = 2/(Window+1), 
	 * 						where EMA(t+1) = alpha*EMA(t) + (1-alpha)*Y  
	 * @param windowSize: the value of the window used in the exponential moving average
	 */
	public void expMovAvSetWindow(int windowSize) {
		expMovingAverage_a = ((2.0/(windowSize+1.0)));
	}
	
	/**
	 * Returns the value of the alpha used in the exponential moving average calculation
	 * @return the double value of alpha
	 */
	public double expMovAvGetA(){
		return this.expMovingAverage_a;
	}
	
	/**
	 * Sets the score of the positive class with value
	 * @param value: the score of the pos class
	 */
	public void setLast_pos_score(double value)
	{
		this.last_pos_score = value;
	}
	
	/**
	 * Gets the score of the positive class with value
	 * @return the score of the pos class
	 */
	public double getLast_pos_score()
	{
		return this.last_pos_score;
	}

	/**
	 * Sets the score of the negative class with value
	 * @param value: the score of the neg class
	 */
	public void setLast_neg_score(double value)
	{
		this.last_neg_score = value;
	}
	
	/**
	 * Gets the score of the negative class with value
	 * @return the score of the neg class
	 */
	public double getLast_neg_score()
	{
		return this.last_pos_score;
	}
}
