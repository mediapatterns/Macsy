package macsy.module.onlineClassification;

/**
 * This class implements a Macsy module that trains a Clasher model for multiclass classification.
 * 
 * 
 * @author Fabon Dzoagang <dzogang.fabon@gmail.com>
 * @since 2015-01-01
 * 
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import macsy.lib.ConfusionMatrix;
import macsy.lib.DataPoint;
import macsy.lib.Helpers;
import macsy.lib.Results;
import macsy.lib.Helpers.ModuleMode;
import macsy.lib.Helpers.Months;
import macsy.lib.onlineLearning.OnlineLearning;
import macsy.lib.onlineLearning.Clasher;
import macsy.module.BaseOnlineLearnerModule;

import org.bson.types.ObjectId;

public class articleClasherClassifier extends BaseOnlineLearnerModule {
	public static final String PROPERTY_CLASHER_UPDATE_ON_ERROR = "CLASHER_UPDATE_ON_ERROR";
	public static final String PROPERTY_CLASHER_UPDATE_BIAS = "CLASHER_UPDATE_BIAS";
	public static final String PROPERTY_CLASHER_REGULARIZATION_FACTOR = "CLASHER_REGULARIZATION_FACTOR";

	protected static final double EPSILON_MIN_CONFIDENCE_VALUE = 0.01;

	protected Clasher _clasher;
	protected BufferedWriter _outPerf;
	protected Map<String, BufferedWriter> _outPerfDetailed;

	protected Results _validationResults;

	public articleClasherClassifier(String propertiesFilename, ModuleMode mode)
			throws Exception {
		super(propertiesFilename, mode);
		_clasher = null;
		_outPerf = null;
		_outPerfDetailed = null;
	}

	protected void initOutputs() throws IOException {
		if (!_recordPerformance)
			return;

		boolean writeHeaders = !new File(_perfPath).exists();
		_outPerf = new BufferedWriter(new FileWriter(_perfPath, true));
		if (writeHeaders) {
			String headers = "id microF macroF microP macroP microR macroR microErr macroErr expMovErr Date Hours";
			_outPerf.write(headers + "\n");
			_outPerf.flush();
		}


		_outPerfDetailed = new HashMap<String, BufferedWriter>();
		 String detailedHeaders =
		 "id Fscore Precision Recall Error TP TN FP FN ProtoL2Norm Date Hours\n";
		for (String label : _strings2labels.keySet()) {
			String filePath = _perfPath.replace(".csv", "") + label + ".csv";
			writeHeaders = !new File(filePath).exists();
			_outPerfDetailed.put(label, new BufferedWriter(new FileWriter(
					filePath, true)));
			if (writeHeaders) {
				_outPerfDetailed.get(label).write(detailedHeaders);
				_outPerfDetailed.get(label).flush();
			}
		}

		_validationResults = new Results(".", _modelPath + ".validation.csv",
				true, true);
		_validationResults
				.println("expMovValidationError TP TN FP FN Ntotal Npos Nneg Date Hours");
	}

	@Override
	protected void initModel() throws Exception {
		_clasher = new Clasher(_strings2labels.keySet(), _window);
		Helpers.outputConsoleTitle("Clasher user settings");

		String msgLabelsInfos = "Labels for classification: ";
		for (String label : _strings2labels.keySet())
			msgLabelsInfos += "[" + label + "] ";
		Helpers.outputConsole(msgLabelsInfos);

		Helpers.outputConsole("Clasher update on error ["
				+ Boolean.valueOf(getProperty(PROPERTY_CLASHER_UPDATE_ON_ERROR)
						.toUpperCase()) + "]");
		Helpers.outputConsole("Clasher Update bias term ["
				+ Boolean.valueOf(getProperty(PROPERTY_CLASHER_UPDATE_BIAS)
						.toLowerCase()) + "]");
		Helpers.outputConsole("Clasher decaying rate ["
				+ Boolean.valueOf(getProperty(PROPERTY_DECAYING_LEARNING_RATE))
				+ "]");
		Helpers.outputConsole("Learning rate [" + _learningRate + "]");
		Helpers.outputConsole("Clasher regularization factor ["
				+ Double.parseDouble(getProperty(PROPERTY_CLASHER_REGULARIZATION_FACTOR))
				+ "]");

		_clasher.loadModel(_modelPath);
		_clasher.setUpdateOnError(Boolean
				.valueOf(getProperty(PROPERTY_CLASHER_UPDATE_ON_ERROR)));
		_clasher.setUpdateBias(Boolean.valueOf(getProperty(
				PROPERTY_CLASHER_UPDATE_BIAS).toLowerCase()));
		_clasher.setDecayingRate(Boolean
				.valueOf(getProperty(PROPERTY_DECAYING_LEARNING_RATE)));
		_clasher.setLamdaReg(Double
				.parseDouble(getProperty(PROPERTY_CLASHER_REGULARIZATION_FACTOR)));

		initOutputs();
	}

	@Override
	protected Integer trainModuleOnSample(DataPoint sample,
			Set<String> articleLabels) throws Exception {
		sample.normalize();
		_clasher.incrementN_overall(1);
		_clasher.train(sample, articleLabels);

		if (_clasher.is_regionsFullyInitialized()) {
			_clasher.printPrototypesInfos();
			String date = _dateFormat.format(Helpers
					.extractDateFromArticleId((ObjectId) sample.getID()));
			saveModulePerfs(date);
		}
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
	protected TreeMap<String, Double> computeModulePredictionOnSample(
			DataPoint sample, boolean updateValidationError) throws Exception {
		sample.normalize();
		if (!_clasher.is_regionsFullyInitialized())
			return null;
		TreeMap<String, Double> histogram = _clasher.predictHistogram(sample);
		for (String label : _strings2labels.keySet())
			if (!histogram.containsKey(label))
				histogram.put(label, 0.0);

		String line = "";
		line += sample.getID() + " ";
		line += histogram.get(Helpers.LABEL_POS_CLASS_STRING) + " ";
		line += Helpers.extractDateFromArticleId((ObjectId) sample.getID());

		if (updateValidationError) {
			_clasher.update_validationError(histogram, sample.getRealLabel());

			double[] confusionMatrix = _clasher.getValidationConfusion();
			double tp = confusionMatrix[0];
			double tn = confusionMatrix[1];
			double fp = confusionMatrix[2];
			double fn = confusionMatrix[3];

			_validationResults.print(_clasher.expMovAvGetValidationError()
					+ " ");
			_validationResults.print(tp + " " + tn + " " + fp + " " + fn + " ");
			_validationResults.print(_clasher.getNValidation() + " "
					+ _clasher.getNpos() + " " + _clasher.getNneg() + " ");
			_validationResults.print(_dateFormat.format(Helpers
					.extractDateFromArticleId((ObjectId) sample.getID())));
			_validationResults.print("\n");
			_validationResults.Flush();
		}
		return histogram;
	}

	@Override
	protected void predictionsToDecisions(Map<String, Double> scores) {
		if (scores == null)
			return;

		Set<String> predictionsThatLeadToNegativeDecisions = new HashSet<String>();
		for (String label : scores.keySet()) {
			Double score = scores.get(label);
			if (!_clasher.is_regionsFullyInitialized()
					|| score < Clasher.DECISION_THRESHOLD)
				predictionsThatLeadToNegativeDecisions.add(label);
		}
		scores.keySet().removeAll(predictionsThatLeadToNegativeDecisions);
	}

	@Override
	protected void saveModulePerfs(String date) throws Exception {
		// Multiclass Macro perfs
		Double macroF = _clasher.statsGetMacroFscore();
		Double macroError = 1 - _clasher.statsGetMacroAccuracy();
		Double macroP = _clasher.statsGetMacroPrecision();
		Double macroR = _clasher.statsGetMacroRecall();

		// Multiclass Micro perfs
		Double microF = _clasher.statsGetMicroFscore();
		ConfusionMatrix microMatrix = _clasher.statsGetMicroMatrix();
		Double globalPrecision = _clasher.statsGetMicroPrecision(microMatrix);
		Double globalRecall = _clasher.statsGetMicroRecall(microMatrix);
		Double globalAccuracy = _clasher.statsGetMicroAccuracy(microMatrix);

		String line = _clasher.statsGetN() + " " + microF + " " + macroF + " "
				+ globalPrecision + " " + macroP + " " + globalRecall + " "
				+ macroR + " " + globalAccuracy + " " + macroError + " "
				+ _clasher.expMovAvGetError() + " " + date;

		_outPerf.write(line + "\n");
		_outPerf.flush();

		// Detailed perfs
		for (String label : _strings2labels.keySet()) {
			 ConfusionMatrix m =
			 _clasher.statsGetDetailedConfusionMatrix(label);
			 Double error = m.getClassificationError();

			 String lineDetailed = _clasher.statsGetN() + " " +
			 m.getFMeasure()
			 + " " + m.getPrecision() + " " + m.getRecall() + " "
			 + error + " " + m.getPositive_given_positive() + " "
			 + m.getNegative_given_negative() + " "
			 + m.getPositive_given_negative() + " "
			 + m.getNegative_given_positive() + " "
			 + _clasher.statsGetPrototypeL2Norm(label) + " " + date;
			_outPerfDetailed.get(label).write(lineDetailed + "\n");
			_outPerfDetailed.get(label).flush();
		}

		String outputLine = "COUNTER : " + _clasher.statsGetN()
				+ "\nMICRO F : " + microF + "\nMACRO F : " + macroF
				+ "\nMICRO P : " + globalPrecision + "\nMACRO P : " + macroP
				+ "\nMICRO R : " + globalRecall + "\nMACRO R : " + macroR
				+ "\nMICRO A : " + globalAccuracy + "\nMACRO A : " + macroError
				+ "\nExpMovErr : " + _clasher.expMovAvGetError()
				+ "\n--------------------------------------------";
		System.out.println(outputLine);
	}

	protected void closeOutputs() throws IOException {
		if (_recordPerformance) {
			_outPerf.close();
			for (String label : _strings2labels.keySet())
				_outPerfDetailed.get(label).close();
		}
		_validationResults.SaveOutput();
	}

	@Override
	protected Double getModuleError() throws Exception {
		return _clasher.expMovAvGetError(); // training error
	}

	@Override
	protected void saveModel() throws Exception {
		_clasher.saveModel(_modelPath);
		closeOutputs();

		//_outScore.close();
	}

	public static void main(String[] args) throws Exception {
		articleClasherClassifier module = new articleClasherClassifier(args[0],
				Helpers.ModuleMode.EXPERIMENT);
		module.setModuleDateInterval(2014, Months.OCT, 1, 2014, Months.DEC, 16);
		module.run();
	}

	@Override
	public OnlineLearning getModel() {
		return _clasher;
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
}
