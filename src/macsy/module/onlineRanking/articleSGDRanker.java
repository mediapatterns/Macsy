package macsy.module.onlineRanking;

/**
 * This class implements a Macsy module that trains discriminative linear model in the Stochastic Gradient Descent framework for preference learning.
 * 
 * 
 * @author Fabon Dzoagang <dzogang.fabon@gmail.com>
 * @since 2015-01-01
 * 
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import macsy.lib.BasicLinearModel;
import macsy.lib.DataPoint;
import macsy.lib.Helpers;
import macsy.lib.Results;
import macsy.lib.Helpers.BinaryLabel;
import macsy.lib.Helpers.FeatureSpace;
import macsy.lib.Helpers.ModuleMode;
import macsy.lib.Helpers.Months;
import macsy.lib.onlineLearning.SGD;
import macsy.lib.preprocessing.DataSampleBuilderForRanking;
import macsy.lib.preprocessing.FeatureStandardizer;
import macsy.module.onlineClassification.articleSGDClassifier;

import org.bson.types.ObjectId;

public class articleSGDRanker extends articleSGDClassifier {
	protected Results _validationResults;
	protected DataSampleBuilderForRanking _data;

	public articleSGDRanker(String propertiesFilename, ModuleMode mode)
			throws Exception {
		super(propertiesFilename, mode);

		_binaryTask = false;
		_rankingTask = true;
		_perceptron = null;
		_learningResults = null;
		_data = new DataSampleBuilderForRanking(false);
	}

	@Override
	protected void initModel() throws Exception {
		super.initModel();
		
		_validationResults = new Results(".", _modelPath + ".validation.csv",
				true, true);
		_validationResults
				.println("expMovValidationError TP TN FP FN Ntotal Npos Nneg Date Hours");
	}

	@Override
	protected void saveModel() throws Exception {
		super.saveModel();
		_validationResults.SaveOutput();
	}

	@Override
	protected boolean collectLearningSamples(DataPoint sample,
			Set<String> articleLabels) throws Exception {

		return _data.collect(sample, articleLabels);
	}

	@Override
	protected Integer trainModuleOnSample(DataPoint sample,
			Set<String> articleLabels) throws Exception {
		/*
		 * articlePerceptronRanker processes samples per days, please make use
		 * of trainModuleOnDay !
		 */
		return 0;

	}

	@Override
	protected boolean enoughSampleForADay() throws Exception {
		return _fSpace != FeatureSpace.CONCEPTS
				|| _data.nbPopularSamples() >= 5;
	}

	@Override
	protected void preProcessInputDay() throws Exception {
		if (_fSpace == FeatureSpace.CONCEPTS) {
			List<DataPoint> dailyLearningData = new ArrayList<DataPoint>();
			dailyLearningData.addAll(_data._popularSamples);
			dailyLearningData.addAll(_data._dislikedSamples);
			FeatureStandardizer.standardize(dailyLearningData);
		}
	}

	@Override
	protected void validateModuleOnDay() throws Exception {
		DataPoint pair = null;
		while (_data.hasNext()) {
			pair = _data.next();
			pair.normalize();
			computeModulePredictionOnSample(pair, true);
		}
		_data.nbPopularSamples();
		_data.nbDislikedSamples();
		_data.reset();
	}

	@Override
	protected Integer trainModuleOnDay() throws Exception {
		DataPoint pair = null;
		while (_data.hasNext()) {
			pair = _data.next();
			pair.normalize();
			BinaryLabel label = BinaryLabel.fromInt(pair.getRealLabel());
			super.trainModuleOnSample(pair, Helpers.hashSet(label.name()));
		}

		int nbPopular = _data.nbPopularSamples();
		int nbDisliked = _data.nbDislikedSamples();
		_data.clear();
		if (pair != null) {
			String date = _dateFormat.format(Helpers
					.extractDateFromArticleId((ObjectId) pair.getID()));

			BasicLinearModel linearModel = (BasicLinearModel) ((SGD) _perceptron)
					.getLinearModel();
			linearModel.setModelFileName(_modelPath + "_words_"
					+ date.split(" ")[0] + ".csv");
			linearModel.saveModel("");
		}
		return nbPopular > 0 ? nbPopular + nbDisliked : 0;
	}

	@Override
	protected TreeMap<String, Double> computeModulePredictionOnSample(
			DataPoint sample, boolean updateValidationError) throws Exception {
		TreeMap<String, Double> score = new TreeMap<String, Double>();
		double prediction = _perceptron.predict(sample);
		score.put(Helpers.LABEL_POS_CLASS_STRING, prediction);

		if (updateValidationError) {
			_perceptron.update_validationError(prediction,
					sample.getRealLabel());

			double[] confusionMatrix = _perceptron.getValidationConfusion();
			double tp = confusionMatrix[0];
			double tn = confusionMatrix[1];
			double fp = confusionMatrix[2];
			double fn = confusionMatrix[3];

			_validationResults.print(_perceptron.expMovAvGetValidationError()
					+ " ");
			_validationResults.print(tp + " " + tn + " " + fp + " " + fn + " ");
			_validationResults
					.print(_perceptron.getNValidation() + " "
							+ _perceptron.getNpos() + " "
							+ _perceptron.getNneg() + " ");
			_validationResults.print(_dateFormat.format(Helpers
					.extractDateFromArticleId((ObjectId) sample.getID())));
			_validationResults.print("\n");

			_validationResults.Flush();
		}
		return score;
	}

	public static void main(String[] args) throws Exception {
		articleSGDRanker module = new articleSGDRanker(args[0],
				Helpers.ModuleMode.EXPERIMENT);
		module.setModuleDateInterval(2010, Months.JAN, 1, 2015, Months.FEB,
				4);
		module.run();
	}
}
