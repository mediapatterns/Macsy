package macsy.lib.onlineLearning;

/**
 * This class implements Stochastic Gradient Algorithms for training linear predictor functions: 
 * 
 *  __Perceptron__
 * The perceptron updates when an error is seen. The update rule is as follows: e.g. prediction: w(t), x(t) + b. 
 * If error, update: w(t + 1) = w(t) + η(desired output − real output)x(t), b(t + 1) = b(t) + η(desired output − real output) where w is the weight vector, 
 * x is the input, b is the bias and η the learning factor.
 * 
 *  __Ridge__
 * This algorithm approximates the center of mass of a class with its exponential smoothing average. 
 * Unlike the algorithms provided in macsy.lib.onlineLearning.SGD.java it learns in a multiclass setting and trains generatively on a 
 * stream. It also has inherent parallel processing capabilities and under mild conditions has been proved to behave well under a compressed
 * 
 *  __Lasso__
 * This algorithm approximates the center of mass of a class with its exponential smoothing average. 
 * Unlike the algorithms provided in macsy.lib.onlineLearning.SGD.java it learns in a multiclass setting and trains generatively on a 
 * stream. It also has inherent parallel processing capabilities and under mild conditions has been proved to behave well under a compressed 
 * 
 * transformation of the original input space.
 * 
 * @author Fabon Dzoagang <dzogang.fabon@gmail.com>
 * @since 2015-01-01
 * 
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import macsy.lib.AUC;
import macsy.lib.BasicLinearModel;
import macsy.lib.DataPoint;
import macsy.lib.Helpers;
import macsy.lib.LinearModel;
import macsy.lib.Results;
import macsy.lib.Helpers.SGDLearningRateStrategy;
import macsy.lib.Helpers.SGDLoss;
import macsy.lib.Helpers.SGDRegularizerFunction;
import macsy.lib.Helpers.SGDTransfertFunction;


public class SGD implements OnlineLearning {
	// variable for the time (timestamp)
	private long timestamp;
	// variable to store the desired precision the user defines. This is used
	// for
	// the extra bias we use in the algorithm (it is set to -1 if we don't wish
	// extra bias)
	private double desiredPrecision;
	// variable to store the extra bias - r (which is subtracted from the actual
	// output)
	private double threshold_for_precisionANDrecall;
	// variable to change the value of the extra bias (r)
	private double change_of_thres;
	// this is the variable given by the user for the learning factor (eta) used
	// in the
	// updating rule (learning process) w(t+1) = w(t) + eta * y(t) * x(t) (see
	// below)
	private double _learningFactor = 0.1;
	// this is a boolean variable for the user to choose if the learning factor
	// will be
	// updated according to the number of the instances per class (different
	// learning
	// factors per class) or not
	private boolean updateLearningFactor;
	// this variables exist to store the taus used for the algorithm in order to
	// make the
	// decision if the updating rule will be applied or not (different per class
	// if
	// necessary)
	private double thresPosMargin;
	private double thresNegMargin;
	// parameters like the window or the alpha for the exponential moving
	// average
	// calculation (we use the formula a = 2/(1+N), where N
	// (expMovingAverage_window) is
	// the number of data we wish to remember and a (expMovingAverage_a) is the
	// smoothing
	// constant used in the rule X(t) = alpha*Y(t) + (1-alpha)*X(t-1), where X
	// is the input and Y
	// the event of interest (e.g. the appearance of error if it's the
	// exponential moving
	// average error, that we compute
	private int expMovingAverage_window;
	private double expMovingAverage_a;
	// variable for storing the error
	private double expMovingAverage_error;
	// boolean variables for the initialise the number of positive or negative
	// instances
	private boolean first_pos;
	private boolean first_neg;
	// counters for the number of positive or negative instances are met
	private double N_hat_Pos = 1.0;
	private double N_hat_Neg = 1.0;
	// Define the positions of positive (1) tag for the tag list
	private static final int POSITIVE_LABEL = 1;
	// matrix to hold statistic information
	private long[] StatisticsMatrix = new long[4];
	// define the positions of TP, TN, FP and FN in the statistics matrix
	private static final int TP = 0;
	private static final int TN = 1;
	private static final int FP = 2;
	private static final int FN = 3;
	// the model used for the algorithm of online learning
	private LinearModel linearModel;
	// initial values for the exponential moving average of the number of
	// pos/neg
	private static double M_pos = 0.0;
	private static double M_neg = 0.0;
	private int N_overall = 0;
	private int epoch = 0;
	// object to hold the information for the area under the curve
	AUC AUC_object;
	private double AUC_value;
	// counters for pocket perceptron
	// # of consecutive correct classification using perceptron weights
	private int run_p;

	// # of total training examples correctly classified by perceptron weights
	private int num_ok_p;

	private int Nvalidation;
	private int validationTP;
	private int validationTN;
	private int validationFP;
	private int validationFN;
	private int validationNpos;
	private int validationNneg;
	private double expMovingAverage_ValidationError; // Micro error

	SGDTransfertFunction _transfer = SGDTransfertFunction.SIGN;
	SGDRegularizerFunction _regularizer = SGDRegularizerFunction.NONE;
	SGDLearningRateStrategy _learningRateStrategy = SGDLearningRateStrategy.CONSTANT;
	boolean _updateBias = false;

	Double _lambdaReg = 1.0;
	DataPoint[] posComponentsOfWForL1;

	Date lastDate = null;

	public void setLearningRateStrategy(
			SGDLearningRateStrategy learningRateStrategy) {
		_learningRateStrategy = learningRateStrategy;
	}

	public void setLamdaReg(Double lambda) {
		_lambdaReg = lambda;
	}

	public void setUpdateBias(boolean updateBias) {
		_updateBias = updateBias;
	}

	public void setTransfertFunction(SGDTransfertFunction transfer) {
		_transfer = transfer;
	}

	public void setRegularizerFunction(SGDRegularizerFunction regularizer) {
		_regularizer = regularizer;
	}

	public void setLoss(SGDLoss loss) {
		setRegularizerFunction(SGDRegularizerFunction.NONE);
		switch (loss) {
		// PERCEPTRON algorithms
		case PERCEPTRON:
			setTransfertFunction(SGDTransfertFunction.SIGN);
			break;
		case PERCEPTRON_L2:
			setTransfertFunction(SGDTransfertFunction.SIGN);
			setRegularizerFunction(SGDRegularizerFunction.L2);
			break;
		// Logistic regression algorithms
		case LOGISTIC_REGRESSION:
			setTransfertFunction(SGDTransfertFunction.LOGISTIC);
			break;
		case LOGISTIC_REGRESSION_L2:
			setTransfertFunction(SGDTransfertFunction.LOGISTIC);
			setRegularizerFunction(SGDRegularizerFunction.NONE);
			break;
		// Linear regression algorithms (including the LASSO)
		case LINEAR_REGRESSION:
			setTransfertFunction(SGDTransfertFunction.LINEAR);
			break;
		case LINEAR_REGRESSION_L2:
		case RIDGE:
			setTransfertFunction(SGDTransfertFunction.LINEAR);
			setRegularizerFunction(SGDRegularizerFunction.L2);
			break;
		case LINEAR_REGRESSION_L1:
		case LASSO:
			setTransfertFunction(SGDTransfertFunction.LINEAR);
			setRegularizerFunction(SGDRegularizerFunction.L1);
			break;
		case HINGE_REGRESSION_L1:
			setTransfertFunction(SGDTransfertFunction.HINGE);
			setRegularizerFunction(SGDRegularizerFunction.L1);
			break;
		case HINGE_REGRESSION_L2:
		case SVM:
			setTransfertFunction(SGDTransfertFunction.HINGE);
			setRegularizerFunction(SGDRegularizerFunction.L2);
			break;
		}
		if (_regularizer == SGDRegularizerFunction.L1) {
			posComponentsOfWForL1 = posComponentsOfWForL1 == null ? new DataPoint[2]
					: posComponentsOfWForL1;
			posComponentsOfWForL1[0] = posComponentsOfWForL1[0] == null ? new DataPoint()
					: posComponentsOfWForL1[0];
			posComponentsOfWForL1[1] = posComponentsOfWForL1[1] == null ? new DataPoint()
					: posComponentsOfWForL1[1];
		}

		Helpers.outputConsoleTitle("SGD algo : [" + loss + "]");
		Helpers.outputConsole("Transfert function [" + _transfer + "]");
		Helpers.outputConsole("Regularizer function [" + _regularizer + "]");
		Helpers.outputConsole("Learning Rate Strategy ["
				+ _learningRateStrategy + "]");
		Helpers.outputConsole("Update Bias [" + _updateBias + "]");
		if (_regularizer != SGDRegularizerFunction.NONE)
			Helpers.outputConsole("Lambda value Reg [" + _lambdaReg + "]");
	}

	public SGD() {
	}

	public SGD(String wFileName, int expMovingAverage_window)
			throws Exception {
		// load the model if it already exists or create an empty file
		loadModel(wFileName);
		// the threshold for decision is set to 0
		threshold_for_precisionANDrecall = 0.0;
		// the change of the threshold (if necessary) is set to 0.00001
		change_of_thres = 0.00001;
		// these variables are to define the first time the number of instances
		// per class
		// are calculated (for initialisation purposes only)
		first_pos = true;
		first_neg = true;
		// create the AUC object
		AUC_object = new AUC();
		this.expMovingAverage_window = expMovingAverage_window;
		setExpMovingAverage_a((2.0 / (expMovingAverage_window + 1.0)));
	}

	/**
	 * This function performs the prediction and the training procedure taking
	 * into account the model, of a given input sample
	 * 
	 * @param sample
	 *            : the input DataPoint
	 * 
	 *            This function also predicts the output of the sample with the
	 *            given model in order to update the model, the error and the
	 *            statistical information
	 */

	protected void regularizeL2(DataPoint w) {
		Map<Integer, Double> features = w.getFeaturesMap();
		for (Map.Entry<Integer, Double> e : features.entrySet()) {
			Integer i = e.getKey();
			Double w_i = e.getValue();
			if (i != 0) {// bias term is stored at index 0
				Double removedFraction = w_i * (_lambdaReg * _learningFactor);
				features.put(i, w_i - removedFraction);
			}
		}
	}

	protected void regularizeL1(DataPoint x) {
		Map<Integer, Double> features = x.getFeaturesMap();
		for (Map.Entry<Integer, Double> e : features.entrySet()) {
			Integer i = e.getKey();
			Double x_i = e.getValue();
			features.put(i, x_i - _lambdaReg * _learningFactor);
		}
	}

	private DataPoint buildWfromClippedL1Weights() {
		// First apply update rule for both positive components of
		// the weighting vector

		Map<Integer, Double> uFeatures = posComponentsOfWForL1[0]
				.getFeaturesMap();
		Map<Integer, Double> vFeatures = posComponentsOfWForL1[1]
				.getFeaturesMap();
		Map<Integer, Double> wFeatures = new TreeMap<Integer, Double>();
		// Enforce positive of both components and compute W as the difference
		// of them
		for (Integer i : uFeatures.keySet()) {
			uFeatures.put(i, Math.max(0.0, uFeatures.get(i))); // FIXME eps
																// precision
																// here ?
			vFeatures.put(i, Math.max(0.0, vFeatures.get(i))); // FIXME eps
																// precision
																// here ?
			wFeatures.put(i, uFeatures.get(i) - vFeatures.get(i));
		}
		return new DataPoint(wFeatures);
	}

	private boolean checkAndRecordError(DataPoint sample) {
		boolean error = sample.getPredictedLabel() != sample.getRealLabel();
		if (!error) { // Correct prediction
			int T = (sample.getPredictedLabel() == Helpers.LABEL_POS_CLASS) ? TP
					: TN;
			getStatisticsMatrix()[T]++;
		} else {// Incorrect prediction
			int F = (sample.getPredictedLabel() == Helpers.LABEL_NEG_CLASS) ? FP
					: FN;
			getStatisticsMatrix()[F]++;
		}
		return error;
	}

	public void updateLearningFactor(int epoch) {
		switch (_learningRateStrategy) {
		case CONSTANT:
			break;
		case DECAYING:
			setLearningFactor(1.0 / (1.0 + _lambdaReg * (double) epoch));
			break;
		case ADAPTIVE:
			setLearningFactor(1.0); // FIXME implements the SGD rule here
			break;
		}
	}

	@Override
	public void train(DataPoint sample) throws Exception {
		// basic info
		double yHat = linearModel.predict(sample);
		sample.setPredictedLabel_Value(yHat);
		Integer predictedLabel = (yHat > 0) ? Helpers.LABEL_POS_CLASS
				: Helpers.LABEL_NEG_CLASS;
		sample.setPredictedLabel(predictedLabel);
		Integer realLabel = sample.getRealLabel();

		// update stats
		updateNum(sample);
		boolean error = checkAndRecordError(sample);

		// compute error value with transfer function
		Double errorValue = null;
		switch (_transfer) {
		case SIGN:
			errorValue = error ? learningProcessSign(realLabel, predictedLabel)
					: 0.0;
			break;
		case LOGISTIC:
			errorValue = learningProcessLogistic(realLabel, yHat);
			break;
		case LINEAR:
			errorValue = learningProcessLinear(realLabel, yHat);
			break;
		case HINGE:
			errorValue = learningProcessHinge(realLabel, yHat);
			break;
		}

		++epoch;
//		Date curDate = Helpers.extractDateFromArticleId((ObjectId) sample
//				.getID());
//		if (lastDate == null || !Helpers.compareDates(curDate, lastDate)) {
//			epoch = 1;
//			lastDate = curDate;
//		}
		updateLearningFactor(epoch);

		// then regularize the weights
		DataPoint wNew = linearModel.getW();
		switch (_regularizer) {
		case NONE:
			break;
			
		case L2:
			regularizeL2(wNew);
			break;
		case L1:
			regularizeL1(posComponentsOfWForL1[0]);
			regularizeL1(posComponentsOfWForL1[1]);
			break;
		}

		// update the bias, if needed, according to the error made and the
		// learning factor
		Double biasUpdate = _updateBias ? errorValue * _learningFactor
				* linearModel.getB() : 0.0;

		// update the weight according to the error made and the learning factor
		if (_regularizer != SGDRegularizerFunction.L1) {
			DataPoint wUpdate = learningProcess(sample, errorValue,
					_learningFactor);
			wNew.addDataPoint(wUpdate);//wUpdate stores the complete gradient reduced by learning rate
		} else {
			DataPoint uUpdate = learningProcess(sample, errorValue,
					_learningFactor);
			DataPoint vUpdate = learningProcess(sample, errorValue,
					-_learningFactor);
			posComponentsOfWForL1[0].addDataPoint(uUpdate);
			posComponentsOfWForL1[1].addDataPoint(vUpdate);
			wNew = buildWfromClippedL1Weights(); //complete gradient reduced by learning rate
		}

		double biasNew = linearModel.getB() + biasUpdate;
		linearModel.setW(wNew);
		if (_updateBias)
			linearModel.setB(biasNew);

		// calculate the error rate (exponential moving error average)
		update_exponentialMovingAverageError(error);
		// update the counter of the negative instances
		update_exponentialMovingAverageCounter(sample.getRealLabel());
		statsGetAUC(sample);
		// if (error)
		// throw new Exception("Done.");
	}

	/**
	 * Perform the learning procedure / updates the w, according to the formula
	 * w(t+1) = w(t) + eta*(d-y(t))*x(t)
	 * 
	 * where t is the time step, eta is the learning factor, d is the correct /
	 * desired output, y is the predicted / actual output and x the input
	 * 
	 * @param X
	 *            : the input feature vector
	 * @param learningFactor
	 *            : the learning factor of learning
	 * @param y_hat
	 *            : the predicted output
	 */

	private DataPoint learningProcess(DataPoint x, double errorValue,
			double etha) throws Exception {
		DataPoint wUpdate = new DataPoint();
		Map<Integer, Double> xFeatures = x.getFeaturesMap();
		Map<Integer, Double> wFeatures = wUpdate.getFeaturesMap();
		for (Map.Entry<Integer, Double> e : xFeatures.entrySet()) {
			Integer i = e.getKey();
			Double x_i = e.getValue();
			double errorAmount = errorValue * x_i * etha;
			wFeatures.put(i, errorAmount);
		}
		return wUpdate;
	}

	private double learningProcessSign(Integer realLabel, Integer predictedLabel)
			throws Exception {
		double errorValue = (realLabel - predictedLabel);
		return errorValue;
	}

	private double learningProcessLogistic(Integer realLabel, double yHat)
			throws Exception {
		double errorValue = (realLabel - Helpers.logisticFunc(yHat));
		return errorValue;
	}

	private double learningProcessLinear(Integer realLabel, double yHat)
			throws Exception {
		double errorValue = (realLabel - yHat);
		return errorValue;
	}

	private double learningProcessHinge(Integer realLabel, double yHat)
			throws Exception {
		double errorValue = realLabel * yHat > 1 ? 0.0 : realLabel;
		return errorValue;
	}

	/**
	 * This function calculates the error value according to the exponential
	 * moving average (i.e. error(i) = alpha * X(i) + (1.0 - alpha)*error(i-1)
	 * where X(i) = 1, if there was an error 0, otherwise)
	 * 
	 * @param wasThereError
	 *            : A boolean variable specifying if there was an error or not
	 * @throws Exception
	 */
	@Override
	public void update_exponentialMovingAverageError(boolean wasThereError)
			throws Exception {
		if (wasThereError) {
			expMovingAverage_error = expMovingAverage_a
					+ (1.0 - expMovingAverage_a) * expMovingAverage_error;
		} else {
			expMovingAverage_error = (1.0 - expMovingAverage_a)
					* expMovingAverage_error;
		}
	}

	public void update_exponentialMovingAverageValidationError(boolean error)
			throws Exception {
		expMovingAverage_ValidationError = expMovingAverage_a * (error ? 1 : 0)
				+ (1.0 - expMovingAverage_a) * expMovingAverage_ValidationError;
	}

	@Override
	public double expMovAvGetValidationError() throws Exception {
		return expMovingAverage_ValidationError;
	}

	public double[] getValidationConfusion() {
		double[] confusionMatrix = { validationTP, validationTN, validationFP,
				validationFN };
		return confusionMatrix;
	}

	public int getNValidation() {
		return Nvalidation;
	}

	public int getNpos() {
		return validationNpos;
	}

	public int getNneg() {
		return validationNneg;
	}

	@Override
	public void update_validationError(double prediction, int realLabel)
			throws Exception {
		++Nvalidation;
		validationNpos += realLabel == 1 ? 1 : 0;
		validationNneg += realLabel == -1 ? 1 : 0;
		validationTP += prediction > 0 && realLabel == 1 ? 1 : 0;
		validationFP += prediction > 0 && realLabel == -1 ? 1 : 0;
		validationTN += prediction <= 0 && realLabel == -1 ? 1 : 0;
		validationFN += prediction <= 0 && realLabel == 1 ? 1 : 0;

		boolean error = prediction > 0 && realLabel == -1 || prediction <= 0
				&& realLabel == 1;

		update_exponentialMovingAverageValidationError(error);
	}

	/**
	 * This function performs (only) the prediction procedure taking into
	 * account the model, of the given input sample
	 * 
	 * @param sample
	 *            : the input DataPoint
	 */
	@Override
	public double predict(DataPoint sample) throws Exception {
		return linearModel.predict(sample);
	}

	/**
	 * This function is used for updating the exponential moving average of the
	 * instances per class
	 * 
	 * @param X
	 *            : the input of the model
	 */
	@Override
	public void updateNum(DataPoint X) {
		// if this is the first time for the positive class initialise
		if (first_pos && X.getRealLabel() == 1) {
			M_pos = linearModel.score(X);
			first_pos = false;
			return;
		}
		// if this is the first time for the negative class initialise
		if (first_neg && X.getRealLabel() == -1) {
			M_neg = linearModel.score(X);
			first_neg = false;
			return;
		}
		// else
		if (X.getRealLabel() == 1) {
			M_pos = expMovingAverage_a * M_pos + (1 - expMovingAverage_a)
					* linearModel.score(X);
		} else {
			M_neg = expMovingAverage_a * M_neg + (1 - expMovingAverage_a)
					* linearModel.score(X);
		}
	}

	public void saveAll(String filename) throws IOException {
		Results logResults = new Results(".", filename + ".log", true, true);

		String str = desiredPrecision + "\t" + linearModel.getB() + "\t"
				+ change_of_thres + "\t" + _learningFactor + "\t"
				+ expMovingAverage_window + "\t" + expMovingAverage_error
				+ "\t" + N_hat_Pos + "\t" + N_hat_Neg + "\t"
				+ StatisticsMatrix[TP] + "\t" + StatisticsMatrix[FP] + "\t"
				+ StatisticsMatrix[TN] + "\t" + StatisticsMatrix[FN] + "\t"
				+ AUC_value + "\t" + N_overall;

		Date curDate = new Date();
		timestamp = curDate.getTime();
		logResults.print(Long.toString(this.timestamp) + "\t");
		logResults.println(str);
		logResults.SaveOutput();
		linearModel.saveModel(filename);
	}

	/**
	 * This function saves the current model to a file
	 * 
	 * @param filename
	 *            :The name of the file for the model to be stored
	 * @throws IOException
	 */
	@Override
	public void saveModel(String filename) throws IOException {

		// filename += filename + ".model";
		filename = filename + ".model";
		linearModel.saveModel(filename);
	}

	public void saveLog(String filename) throws IOException {

		Results logResults = new Results(".", filename + ".log", true, true);

		String str = desiredPrecision + "\t" + threshold_for_precisionANDrecall
				+ "\t" + change_of_thres + "\t" + _learningFactor + "\t"
				+ expMovingAverage_window + "\t" + expMovingAverage_error
				+ "\t" + N_hat_Pos + "\t" + N_hat_Neg + "\t"
				+ getStatisticsMatrix()[TP] + "\t" + getStatisticsMatrix()[FP]
				+ "\t" + getStatisticsMatrix()[TN] + "\t"
				+ getStatisticsMatrix()[FN] + "\t" + AUC_value + "\t"
				+ N_overall;

		Date curDate = new Date();
		timestamp = curDate.getTime();
		logResults.print(Long.toString(this.timestamp) + "\t");
		logResults.println(str);
		logResults.SaveOutput();

	}

	/**
	 * This function stores the header for the statistics to a file (it is used
	 * only when the file does not already exists).
	 * 
	 * @param filename
	 *            :The filename the information of statistics will be stored
	 */
	@Override
	public void writeHeader(String filename) throws IOException {
		// WRITE HEADER OF LOG FILE
		File loginfoFilename = new File(filename + ".log");

		BufferedWriter output;

		output = new BufferedWriter(new FileWriter(loginfoFilename));

		String header = "Timestamp \t desPrec \t b \t change_thres \t "
				+ "eta \t EMAwin \t EMAerror \t N_pos \t N_neg \t "
				+ "TP \t FP \t TN \t FN \t AUC \t N_overall\n";
		output.write(header);

		output.close();
		return;
	}

	/**
	 * This function load the already stored model (if available) and the
	 * statistics of the previous run (metaInfo).
	 * 
	 * @param filename
	 *            :The filename the model will be stored
	 */
	@Override
	public void loadModel(String filename) {
		try {
			// initialise the weights (if the model is not already saved make
			// one with zeros)
			linearModel = new BasicLinearModel(filename + ".model");

			// LOAD LOG FILE
			File loginFilename = new File(filename + ".log");

			// counters for the number of positive or negative instances are met
			N_hat_Pos = 1.0;
			N_hat_Neg = 1.0;

			// No voc return.
			if (!loginFilename.exists()) {
				return;
			}

			BufferedReader input;

			input = new BufferedReader(new FileReader(loginFilename));

			// LOAD HEADER
			String line = null, tmp;

			while ((tmp = input.readLine()) != null) {
				if (!tmp.equals("")) {
					line = tmp;
				}
			}
			// line = input.readLine();
			String toks[] = line.split("\t");
			if (toks[0].equalsIgnoreCase("Timestamp ")) {
				return;
			}
			timestamp = Long.parseLong(toks[0]);
			desiredPrecision = Double.parseDouble(toks[1]);
			threshold_for_precisionANDrecall = Double.parseDouble(toks[2]);
			change_of_thres = Double.parseDouble(toks[3]);
			_learningFactor = Double.parseDouble(toks[4]);
			expMovingAverage_window = Integer.parseInt(toks[5]);
			expMovingAverage_error = Double.parseDouble(toks[6]);
			N_hat_Pos = Double.parseDouble(toks[7]);
			N_hat_Neg = Double.parseDouble(toks[8]);
			StatisticsMatrix[TP] = Long.parseLong(toks[9]);
			StatisticsMatrix[FP] = Long.parseLong(toks[10]);
			StatisticsMatrix[TN] = Long.parseLong(toks[11]);
			StatisticsMatrix[FN] = Long.parseLong(toks[12]);
			AUC_value = Double.parseDouble(toks[13]);
			setExpMovingAverage_a((2.0 / (expMovingAverage_window + 1.0)));
			N_overall = Integer.parseInt(toks[14]);

			input.close();
		} catch (Exception e1) {

			e1.printStackTrace();
		}
	}

	/**
	 * Initialise matrix which holds statistics
	 */
	@Override
	public void statsReset() {
		for (int i = 0; i < 4; i++) {
			StatisticsMatrix[i] = 0;
		}
	}

	/**
	 * Returns the precision taking into account the confusion matrix
	 */
	@Override
	public double statsGetPrecision() {
		if (getStatisticsMatrix()[TP] + getStatisticsMatrix()[FP] != 0) {
			double precision = (double) getStatisticsMatrix()[TP]
					/ (double) (getStatisticsMatrix()[TP] + getStatisticsMatrix()[FP]);
			return precision;
		} else {
			return 1.0;
		}
	}

	/**
	 * Returns the value of the AUC of our experiment
	 * 
	 * @throws Exception
	 */
	private void statsGetAUC(DataPoint sample) throws Exception {
		if (sample.getRealLabel() >= 0) {
			AUC_object.setLast_pos_score(linearModel.predict(sample));
		} else {
			AUC_object.setLast_neg_score(linearModel.predict(sample));
		}

		// System.out.println(AUC_object.getLast_pos_score()+ " , " +
		// AUC_object.getLast_neg_score());

		AUC_object.expAUC_addPoint(AUC_object.getLast_pos_score() > AUC_object
				.getLast_neg_score());

		AUC_value = AUC_object.expMovAvGetAUC();
	}

	/**
	 * Returns the recall taking into account the confusion matrix
	 */
	@Override
	public double statsGetRecall() {
		if (getStatisticsMatrix()[TP] + getStatisticsMatrix()[FN] != 0) {
			return (double) getStatisticsMatrix()[TP]
					/ (double) (getStatisticsMatrix()[TP] + getStatisticsMatrix()[FN]);
		} else {
			return 1.0;
		}
	}

	/**
	 * Returns the number of the negative instances taking into account the
	 * confusion matrix
	 */
	@Override
	public long statsGetN() {
		long sum = 0;
		for (int i = 0; i < 4; i++) {
			sum += getStatisticsMatrix()[i];
		}
		return sum;
	}

	/**
	 * Returns the true positive according to the confusion matrix
	 */
	@Override
	public long statsGetTP() {
		return getStatisticsMatrix()[TP];
	}

	/**
	 * Returns the false positive according to the confusion matrix
	 */
	@Override
	public long statsGetFP() {
		return getStatisticsMatrix()[FP];
	}

	/**
	 * Returns the false negative according to the confusion matrix
	 */
	@Override
	public long statsGetFN() {
		return getStatisticsMatrix()[FN];
	}

	/**
	 * Returns the true negative according to the confusion matrix
	 */
	@Override
	public long statsGetTN() {
		return getStatisticsMatrix()[TN];
	}

	/**
	 * This function update the threshold for the decision taking into account
	 * the desired precision
	 * 
	 * @param precision
	 *            :The value of the current precision
	 */
	private void updateThres(double precision) {
		if (precision < desiredPrecision) {
			threshold_for_precisionANDrecall += change_of_thres;
		} else {
			threshold_for_precisionANDrecall -= change_of_thres;
		}
	}

	/**
	 * This function informs the counters for the positives or negatives
	 * instances according to the exponential moving average (i.e. counter(i) =
	 * X(i) + (1.0 - alpha)*counter(i-1) where X(i) = 1, if there was a positive
	 * input 0, otherwise - for the positive counter and the opposite for the
	 * negative ones)
	 * 
	 * @param whichClass
	 *            : A variable specifying in which class the instance belongs
	 * @throws Exception
	 */
	private void update_exponentialMovingAverageCounter(int whichClass)
			throws Exception {
		if (whichClass == POSITIVE_LABEL) {
			// N_hat_Pos = expMovingAverage_a + (1.0 -
			// expMovingAverage_a)*N_hat_Pos;
			N_hat_Pos = 1.0 + (1.0 - expMovingAverage_a) * N_hat_Pos;

			N_hat_Neg = (1.0 - expMovingAverage_a) * N_hat_Neg;
		} else {
			N_hat_Pos = (1.0 - expMovingAverage_a) * N_hat_Pos;

			// N_hat_Neg = expMovingAverage_a + (1.0 -
			// expMovingAverage_a)*N_hat_Neg;
			N_hat_Neg = 1.0 + (1.0 - expMovingAverage_a) * N_hat_Neg;
		}
	}

	/**
	 * This function sets the N (window)-number of instances to remember for the
	 * calculation of the exponential moving average, and the alpha (the
	 * smoothing factor)
	 * 
	 * @param windowSize
	 *            : the size of the memory window
	 */
	@Override
	public void expMovAvSetWindow(int windowSize) {
		expMovingAverage_window = windowSize;
		setExpMovingAverage_a((2.0 / (expMovingAverage_window + 1.0)));

		AUC_object.expMovAvSetWindow(windowSize);
	}

	/**
	 * This function outputs in the proper file the confusion matrix
	 */
	@Override
	public void statsPrintConfusionMatrix() {
		String strTXT = "\t\tActual class\n" + "\t\tP\tN\n" + "Pred.P\t"
				+ statsGetTP() + "\t" + statsGetFP() + " \n ";
		strTXT = strTXT + "Pred.N\t" + statsGetFN() + "\t" + statsGetTN()
				+ " \n ";

		System.out.println(strTXT);
	}

	/**
	 * Returns the threshold r used for the decision (<w*x + b> >= r)
	 */
	@Override
	public double getDecisionThreshold() {
		return threshold_for_precisionANDrecall;
	}

	/**
	 * Returns the current value of the exponential moving error average
	 */
	@Override
	public double expMovAvGetError() {
		return expMovingAverage_error;
	}

	/**
	 * This function sets a value for the alpha used in the calculation of the
	 * exponential moving average (X(t) = alpha*Y(t) + (1-alpha)*X(t-1))
	 * 
	 * @param b
	 *            : the value for the alpha to be set
	 */
	private void setExpMovingAverage_a(double b) {
		expMovingAverage_a = b;
	}

	/**
	 * This function sets a value for the desired precision we wish to have in
	 * the classification method
	 * 
	 * @param presicion
	 *            : the value for the desired precision to be set
	 */
	@Override
	public void setDesiredPrecision(double precision) {
		desiredPrecision = precision;
	}

	/**
	 * The first value of the exponential moving average (initialisation)
	 */
	@Override
	public void expMovAvReset() throws Exception {
		// initial value for the error
		// expMovingAverage_error = 1.0;
		expMovingAverage_error = 0.0;
	}

	/**
	 * Returns the current value of exponential moving average of the number of
	 * pos
	 */
	@Override
	public double expMovAvGetPositives() {
		return N_hat_Pos;
	}

	/**
	 * Returns the current value of exponential moving average of the number of
	 * neg
	 */
	@Override
	public double expMovAvGetNegatives() {
		return N_hat_Neg;
	}

	/**
	 * Returns the model used in the algorithm
	 */
	@Override
	public LinearModel getLinearModel() {
		// TODO: This should return a deep copy!!!
		return linearModel;
	}

	/**
	 * Returns the value of the desired precision
	 */
	@Override
	public double getDesiredPrecision() {
		return desiredPrecision;
	}

	/**
	 * Sets the decision threshold (this function should not be used -for now-
	 * cause the thres is set automatically)
	 */
	@Override
	public void setDecisionThreshold(double threshold) throws Exception {
		throw new Exception("Threshold is set automatically");
		// threshold_for_precisionANDrecall = threshold;
	}

	/**
	 * Sets the learning factor (eta) with the value provided by its input
	 * 
	 * @param learningFactor
	 *            : the value we wish to set the eta
	 */
	@Override
	public void setLearningFactor(double learningFactor) {
		_learningFactor = learningFactor;
	}

	/**
	 * Sets the positive threshold margin (tau) with the value provided by its
	 * input
	 * 
	 * @param margin
	 *            : the value we wish to set the tau
	 */
	@Override
	public void setPosMargin(double margin) {
		this.thresPosMargin = margin;
	}

	/**
	 * Sets the negative threshold margin (tau) with the value provided by its
	 * input
	 * 
	 * @param margin
	 *            : the value we wish to set the tau
	 */
	@Override
	public void setNegMargin(double margin) {
		this.thresNegMargin = -margin;
	}

	/**
	 * Returns the learning factor (eta) of the algorithm
	 */
	@Override
	public double getLearningFactor() {
		return _learningFactor;
	}

	/**
	 * Returns true if the user wish to update the learning factors per class
	 * and false if not
	 */
	@Override
	public boolean getupdateLearningFactor() {
		return this.updateLearningFactor;
	}

	/**
	 * Sets true if the user wish to update the learning factors per class and
	 * false if not
	 */
	@Override
	public void setUpdateLearningFactor(boolean value) {
		this.updateLearningFactor = value;
	}

	/**
	 * Returns the bias of the model (<wx+b>, where b is the bias)
	 */
	@Override
	public double getBias() {
		return linearModel.getB();
	}

	/**
	 * Returns the value of the AUC
	 */
	@Override
	public double getAUC() {
		return AUC_value;
	}

	/**
	 * Sets the number of the overall data that were classified
	 * 
	 * @param number
	 */
	@Override
	public void incrementN_overall(int addNumber) {
		N_overall += addNumber;
	}

	/**
	 * @return the num_ok_p
	 */
	public int getNum_ok_p() {
		return num_ok_p;
	}

	/**
	 * @return the run_p
	 */
	public int getRun_p() {
		return run_p;
	}

	/**
	 * @param run_p
	 *            the run_p to set
	 */
	public void setRun_p(int run_p) {
		this.run_p = run_p;
	}

	/**
	 * @param num_ok_p
	 *            the num_ok_p to set
	 */
	public void setNum_ok_p(int num_ok_p) {
		this.num_ok_p = num_ok_p;
	}

	/**
	 * @return the StatisticsMatrix
	 */
	public long[] getStatisticsMatrix() {
		return StatisticsMatrix;
	}

}
