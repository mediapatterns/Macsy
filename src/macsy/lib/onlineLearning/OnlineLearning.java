package macsy.lib.onlineLearning;

import java.io.IOException;

import macsy.lib.*;


public interface OnlineLearning {
	/**
	 * Sets the Learning Factor equal to the input learnFactor
	 */
	void setLearningFactor(double learnFactor);
	
	/**
	 * Sets the Learning Factor equal to the input learnFactor
	 */
	double getLearningFactor();
	
	/**
	 * Sets the positive margin (tau) equal to the input margin
	 * @throws Exception 
	 */
	void setPosMargin(double margin) throws Exception;
	
	/**
	 * Sets the negative margin (tau) equal to the input margin
	 */
	void setNegMargin(double margin);
	
	/**
	 * Uses the DataPoint x as a sample and the x.getRealLabel() as label.
	 */
	void train(DataPoint x) throws Exception;;
	
	/**
	 * Predicts the label/value of x based on the learned model.
	 * @param x
	 * @return the label
	 * @throws Exception 
	 */
	double predict(DataPoint x) throws Exception;
	
	
	/**
	 * Saves the model and the statistics (and all other internal variables).
	 * @param filename
	 */
	void saveModel(String filename) throws IOException;
	
	/**
	 * This function stores the header for the statistics to a file 
	 * (it is used only when the file does not already exists).
	 * 
	 * @param filename:The filename the information of statistics will be stored
	 */
	public void writeHeader(String filename) throws IOException;
	
	/**
	 * Loads the model and the statistics
	 * @param filename
	 */
	void loadModel(String filename);

	/**
	 * Returns the model used in the algorithm 
	 * @return the model
	 */
	LinearModel getLinearModel();
	
	/**
	 * Sets the minimum desired precision.
	 * After the desired precision is reached then the recall is optimised.
	 *  
	 */
	void setDesiredPrecision(double precision);
	
	/**
	 * Returns the value of the desired precision (for the use of the ultra bias - r)
	 * @return the desired precision
	 */
	double getDesiredPrecision();
	
	/**
	 * Returns the ultra bias r used for the decision (<w*x + b> >= r) 
	 * @return the value of the ultra bias
	 */
	double getDecisionThreshold();
	
	/**
	 * Sets the decision threshold 
	 * (this function should not be used -for now- cause the thres is set automatically)
	 * @param threshold : the value of the threshold
	 * @throws Exception
	 */
	void setDecisionThreshold(double threshold) throws Exception;
	
	/**
	 * Returns the number of training samples (encoutered so far).
	 * @return
	 */
	long statsGetN();

	/**
	 * Sets TP/TN/FN/FP/N = 0.
	 * 
	 */
	void statsReset();

	/**
	 * Returns the number of TPs (encountered so far).
	 */
	long statsGetTP();
	/**
	 * Returns the number of FPs (encountered so far).
	 */
	long statsGetFP();
	/**
	 * Returns the number of FNs (encountered so far).
	 */
	long statsGetFN();
	/**
	 * Returns the number of TNs (encountered so far).
	 */
	long statsGetTN();
	
	/**
	 * Prints in system out the confusion matrix of our experiment
	 * @throws Exception
	 */
	void statsPrintConfusionMatrix() throws Exception;
	
	/**
	 * Returns the precision of our experiment
	 * @throws Exception
	 */
	double statsGetPrecision() throws Exception;
	
	
	/**
	 * Returns the recall of our experiment
	 * @throws Exception
	 */
	double statsGetRecall() throws Exception;
	
	/**
	 * Sets the size of the moving average window.
	 * 
	 * In the formula
	 * X_new = lambda * current + (1-lambda) * X_old
	 * where lambda = 2/(windowSize+1) 
	 * 
	 */
	void expMovAvSetWindow(int windowSize) throws Exception;
	
	void update_exponentialMovingAverageError(boolean wasThereError) throws Exception;
	
	/**
	 * Resets the error of moving Av Errors = 1.0
	 * @throws Exception
	 */
	void expMovAvReset() throws Exception;
	
	/**
	 * Returns the Mov Average error.
	 * @return
	 * @throws Exception
	 */
	double expMovAvGetError() throws Exception;
	double expMovAvGetValidationError() throws Exception;
	
	/**
	 * Returns the exponential moving average for the counter of the positive instances
	 * @throws Exception
	 */
	double expMovAvGetPositives() throws Exception;
	
	/**
	 * Returns the exponential moving average for the counter of the negative instances
	 * @throws Exception
	 */
	double expMovAvGetNegatives() throws Exception;
	
	public void updateNum(DataPoint X) throws Exception;
	
	/**
	 *
	 * Returns the bias of the model (<wx+b>, where b is the bias)
	 * @return the bias
	 */
	public double getBias();
	
	/**
	 *
	 * Returns true if the user wish to update the learning factors per class 
	 * and false if not
	 * @return the boolean value if the user wish updating learning factor of not
	 */
	public boolean getupdateLearningFactor();
	
	/**
	 * Sets true if the user wish to update the learning factors per class 
	 * and false if not
	 * @param value: the boolean value of the decision whether we update the 
	 * learning factor or not
	 */
	public void setUpdateLearningFactor(boolean value);
	
	public double getAUC();
	
	public void incrementN_overall(int addNumber);

	public void update_validationError(double prediction, int realLabel) throws Exception;

	double[] getValidationConfusion();

	int getNValidation();

	int getNneg();

	int getNpos();
}