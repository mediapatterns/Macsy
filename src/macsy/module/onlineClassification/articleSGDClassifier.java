package macsy.module.onlineClassification;

/**
 * This class implements a Macsy module that trains discriminative linear model in the Stochastic Gradient Descent framework for binary classification.
 * 
 * 
 * @author Fabon Dzoagang <dzogang.fabon@gmail.com>
 * @since 2015-01-01
 * 
 */

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import macsy.lib.BasicLinearModel;
import macsy.lib.DataPoint;
import macsy.lib.Helpers;
import macsy.lib.Results;
import macsy.lib.Helpers.BinaryLabel;
import macsy.lib.Helpers.ModuleMode;
import macsy.lib.Helpers.Months;
import macsy.lib.Helpers.SGDLearningRateStrategy;
import macsy.lib.Helpers.SGDLoss;
import macsy.lib.onlineLearning.OnlineLearning;
import macsy.lib.onlineLearning.SGD;
import macsy.module.BaseOnlineLearnerModule;

import org.bson.types.ObjectId;

public class articleSGDClassifier extends BaseOnlineLearnerModule {
	public static final String PROPERTY_SGD_LOSS = "SGD_LOSS";
	public static final String PROPERTY_SGD_UPDATE_BIAS = "SGD_UPDATE_BIAS";
	public static final String PROPERTY_SGD_REGULARIZATION_FACTOR = "SGD_REGULARIZATION_FACTOR";
	public static final String PROPERTY_SGD_LEARNING_RATE_STRATEGY = "SGD_LEARNING_RATE_STRATEGY";

	protected OnlineLearning _perceptron;
	protected Results _learningResults;

	public articleSGDClassifier(String propertiesFilename,
			ModuleMode mode) throws Exception {
		super(propertiesFilename, mode);

		_binaryTask = true;
		_perceptron = null;
		_learningResults = null;
	}

	@Override
	protected void initModel() throws Exception {
		_learningResults = new Results(".", _perfPath, true, true);
		_learningResults.println(Helpers.HEADER + "error \t Date Hours");

		_perceptron = new SGD(_modelPath, _window);
		_perceptron.writeHeader(_modelPath);
		_perceptron.setUpdateLearningFactor(false);
		_perceptron.setDesiredPrecision(-1);
		_perceptron.setLearningFactor(_learningRate);
		_perceptron.expMovAvSetWindow(_window);

		Helpers.outputConsoleTitle("Stochastic Gradient Descent user settings");
		Helpers.outputConsole("SGD LOSS ["
				+ SGDLoss.valueOf(getProperty(PROPERTY_SGD_LOSS).toUpperCase())
				+ "]");
		Helpers.outputConsole("SGD Update bias term ["
				+ Boolean.valueOf(getProperty(PROPERTY_SGD_UPDATE_BIAS)
						.toLowerCase()) + "]");
		Helpers.outputConsole("SGD Learning Rate Strategy ["
				+ SGDLearningRateStrategy.valueOf(getProperty(
						PROPERTY_SGD_LEARNING_RATE_STRATEGY).toUpperCase())
				+ "]");
		Helpers.outputConsole("SGD decaying rate ["
				+ Boolean.valueOf(getProperty(PROPERTY_DECAYING_LEARNING_RATE))
				+ "]");
		Helpers.outputConsole("Learning rate [" + _learningRate + "]");
		Helpers.outputConsole("SGD regularization factor ["
				+ Double.parseDouble(getProperty(PROPERTY_SGD_REGULARIZATION_FACTOR))
				+ "]");

		((SGD) _perceptron).setUpdateBias(Boolean
				.valueOf(getProperty(PROPERTY_SGD_UPDATE_BIAS).toLowerCase()));
		((SGD) _perceptron)
				.setLearningRateStrategy(SGDLearningRateStrategy
						.valueOf(getProperty(
								PROPERTY_SGD_LEARNING_RATE_STRATEGY)
								.toUpperCase()));
		((SGD) _perceptron).setLamdaReg(Double
				.parseDouble(getProperty(PROPERTY_SGD_REGULARIZATION_FACTOR)));
		((SGD) _perceptron).setLoss(SGDLoss.valueOf(getProperty(
				PROPERTY_SGD_LOSS).toUpperCase()));
	}

	@Override
	public OnlineLearning getModel() {
		return _perceptron;
	}

	@Override
	protected void saveModel() throws Exception {
		((SGD) _perceptron).saveAll(_modelPath);
		_learningResults.SaveOutput();
	}

	@Override
	protected Integer trainModuleOnSample(DataPoint sample,
			Set<String> articleLabels) throws Exception {
		sample.normalize();
		_perceptron.incrementN_overall(1);

		/*
		 * Only singly label samples here
		 */
		BinaryLabel label = BinaryLabel
				.valueOf(articleLabels.iterator().next());
		sample.setRealLabel(label.numericValue);

		_perceptron.train(sample);
		String date = _dateFormat.format(Helpers
				.extractDateFromArticleId((ObjectId) sample.getID()));
		saveModulePerfs(date);
		return 1;

	}

	@Override
	protected Integer trainModuleOnDay() throws Exception {
		/*
		 * articleClasherClassifer process samples as they arrive, implement
		 * trainModuleOnSample !
		 */
		return 0;
	}

	@Override
	protected Double getModuleError() throws Exception {
		return _perceptron.expMovAvGetError();
	}

	@Override
	protected TreeMap<String, Double> computeModulePredictionOnSample(
			DataPoint sample, boolean updateValidationError) throws Exception {
		TreeMap<String, Double> score = new TreeMap<String, Double>();
		double prediction = _perceptron.predict(sample);
		score.put(Helpers.LABEL_POS_CLASS_STRING, prediction);

		return score;
	}

	@Override
	protected void predictionsToDecisions(Map<String, Double> scores) {
		Set<String> predictionsThatLeadToNegativeDecisions = new HashSet<String>();
		for (String label : scores.keySet())
			if (scores.get(label) < _perceptron.getDecisionThreshold())
				predictionsThatLeadToNegativeDecisions.add(label);
		scores.keySet().removeAll(predictionsThatLeadToNegativeDecisions);
	}

	private String calculatePrintStatistics() throws Exception {
		double precision = 0.0, recall = 0.0, f_measure = 0;

		precision = _perceptron.statsGetPrecision();
		recall = _perceptron.statsGetRecall();
		if (precision + recall != 0)
			f_measure = 2 * precision * recall / (precision + recall);
		else
			f_measure = 0.0;

		return precision + " \t " + recall + " \t " + f_measure + " \t ";
	}

	private void writingStatisticsInFile(String str, String date)
			throws Exception {
		double N_hat_Pos = _perceptron.expMovAvGetPositives();
		double N_hat_Neg = _perceptron.expMovAvGetNegatives();
		double display_ALL = N_hat_Pos + N_hat_Neg;

		double error = (double) (_perceptron.statsGetFP() + _perceptron
				.statsGetFN())
				/ (double) (_perceptron.statsGetTP() + _perceptron.statsGetTN()
						+ _perceptron.statsGetFP() + _perceptron.statsGetFN());

		_learningResults.print(str);
		_learningResults.print(Long.toString(_perceptron.statsGetTP()));
		_learningResults.print(" \t ");
		_learningResults.print(Long.toString(_perceptron.statsGetFP()));
		_learningResults.print(" \t ");
		_learningResults.print(Long.toString(_perceptron.statsGetTN()));
		_learningResults.print(" \t ");
		_learningResults.print(Long.toString(_perceptron.statsGetFN()));
		_learningResults.print(" \t ");
		_learningResults.print(Double.toString(_perceptron.expMovAvGetError()));
		_learningResults.print(" \t ");
		_learningResults.print(Double.toString(N_hat_Pos));
		_learningResults.print(" \t ");
		_learningResults.print(Double.toString(N_hat_Neg));
		_learningResults.print(" \t ");
		_learningResults.print(Double
				.toString((double) N_hat_Pos / display_ALL));
		_learningResults.print(" \t ");
		_learningResults.print(Double.toString(_perceptron.getLearningFactor()));
		_learningResults.print(" \t ");
		_learningResults.print(Double.toString(_perceptron.getBias()));
		_learningResults.print(" \t ");
		BasicLinearModel m = (BasicLinearModel) _perceptron.getLinearModel();
		m.calculateMagnitude();
		_learningResults.print(Double.toString(m.getMagnitude()));
		_learningResults.print(" \t ");
		_learningResults.print(Double.toString(error));
		_learningResults.print(" \t ");
		_learningResults.println(date);
		_learningResults.Flush();
	}

	@Override
	protected void saveModulePerfs(String date) throws Exception {
		_perceptron.statsPrintConfusionMatrix();
		writingStatisticsInFile(calculatePrintStatistics(), date);
	}
	

	@Override
	protected void validateModuleOnDay() throws Exception {
		/*
		 * validation occurs on the stream for plain classification
		 */
	}

	@Override
	protected void preProcessInputDay() throws Exception {
		/*
		 * No additional preprocessing for plain classification
		 */
	}

	@Override
	protected boolean enoughSampleForADay() throws Exception {
		/*
		 * No data accumulation for plain classification
		 */
		return false;
	}

	public static void main(String[] args) throws Exception {
		articleSGDClassifier module = new articleSGDClassifier(
				args[0], Helpers.ModuleMode.EXPERIMENT);
		module.setModuleDateInterval(2014, Months.OCT, 1, 2014, Months.DEC, 16);
		module.run();
	}

}
