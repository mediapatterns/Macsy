package macsy.module.onlineRanking;

/**
 * This class implements a Macsy module that trains a Clasher model for preference learning.
 * 
 * 
 * @author Fabon Dzoagang <dzogang.fabon@gmail.com>
 * @since 2015-01-01
 * 
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import macsy.lib.BasicLinearModel;
import macsy.lib.DataPoint;
import macsy.lib.Helpers;
import macsy.lib.Helpers.BinaryLabel;
import macsy.lib.Helpers.FeatureSpace;
import macsy.lib.Helpers.ModuleMode;
import macsy.lib.preprocessing.DataSampleBuilderForRanking;
import macsy.lib.preprocessing.FeatureStandardizer;
import macsy.module.onlineClassification.articleClasherClassifier;

import org.bson.types.ObjectId;

public class articleClasherRanker extends articleClasherClassifier {

	protected DataSampleBuilderForRanking _data;

	public articleClasherRanker(String propertiesFilename, ModuleMode mode)
			throws Exception {
		super(propertiesFilename, mode);
		_rankingTask = true;
		_data = new DataSampleBuilderForRanking();

	}

	@Override
	protected Double getModuleError() throws Exception {
		/**
		 * We do not use the training error here due to the pairwise training:
		 * samples are highly dependent of one another during each day. We
		 * measure error on new data given past model.
		 */
		return _clasher.expMovAvGetValidationError(); // test error
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
		 * articleClasherRanker processes samples per days, please make use of
		 * trainModuleOnDay !
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
			super.computeModulePredictionOnSample(pair, true);
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
			BinaryLabel label = pair.getRealLabel() == BinaryLabel.POSITIVE.numericValue ? BinaryLabel.POSITIVE
					: BinaryLabel.NEGATIVE;
			super.trainModuleOnSample(pair, Helpers.hashSet(label.name()));
		}

		int nbPopular = _data.nbPopularSamples();
		int nbDisliked = _data.nbDislikedSamples();
		_data.clear();

		if (pair != null) {
			String date = _dateFormat.format(Helpers
					.extractDateFromArticleId((ObjectId) pair.getID()));

			BasicLinearModel linearModel = (BasicLinearModel) _clasher
					.getDerivedLinearModel();
			linearModel.setModelFileName(_modelPath + "_words_"
					+ date.split(" ")[0] + ".csv");
			linearModel.saveModelNonSparse("");
		}
		return nbPopular * nbDisliked;
	}

	public static void main(String[] args) throws Exception {
		try {
			articleClasherRanker module = new articleClasherRanker(args[0],
					Helpers.ModuleMode.EXPERIMENT);
			module.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
