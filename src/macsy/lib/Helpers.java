package macsy.lib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BlackBoard;
import macsy.blackBoardsSystem.BlackBoardDateBased;
import macsy.lib.preprocessing.NGPreprocessing;
import macsy.module.featuresExtractorTFIDF.NGIndexer;
import macsy.module.onlineClassifier.OnlineClassifier_StorageLayer;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;

public class Helpers {

	/**
	 * misc
	 */

	public static Double logisticFunc(Double x) {
		return 1.0 / (1 + Math.exp(-x));
	}

	public static <T> Set<T> hashSet(T val) {
		Set<T> res = new HashSet<T>();
		res.add(val);
		return res;
	}

	/**
	 * Date and time helpers functions
	 */

	public static Date extractDateFromArticleId(String id) {
		return new Date(new ObjectId(id).getTime());
	}

	public static Date extractDateFromArticleId(ObjectId id) {
		return new Date(id.getTime());
	}

	public static int extractArticleYear(String id) {
		Date date = extractDateFromArticleId(id);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		return cal.get(Calendar.YEAR);
	}

	public static void getRangeDateSince(int y, int m, int d, Date[] range) {
		Date fromDate = new GregorianCalendar(y, m, d, 0, 0, 1).getTime();
		
		Calendar cal = new GregorianCalendar(y, m, d);
		cal.add(Calendar.YEAR, 1);
		cal.add(Calendar.DAY_OF_MONTH, -1);
		Date toDate = new GregorianCalendar(cal.get(Calendar.YEAR),
				cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 23,
				59, 59).getTime();
		range[0] = fromDate;
		range[1] = toDate;
	}

	public static boolean compareDates(Date date1, Date date2) {

		if (date1 == null || date2 == null)
			return false;

		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(date1);
		cal2.setTime(date2);
		return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
				&& cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
				&& cal1.get(Calendar.DAY_OF_YEAR) == cal2
						.get(Calendar.DAY_OF_YEAR);
	}

	public static boolean inferiorUpToMinutes(Date date1, Date date2) {

		if (date1 == null || date2 == null)
			return false;

		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(date1);
		cal2.setTime(date2);
		return cal1.get(Calendar.YEAR) <= cal2.get(Calendar.YEAR)
				&& cal1.get(Calendar.MONTH) <= cal2.get(Calendar.MONTH)
				&& cal1.get(Calendar.DAY_OF_YEAR) <= cal2
						.get(Calendar.DAY_OF_YEAR)
				&& cal1.get(Calendar.HOUR_OF_DAY) <= cal2
						.get(Calendar.HOUR_OF_DAY)
				&& cal1.get(Calendar.MINUTE) <= cal2.get(Calendar.MINUTE);
	}

	public static boolean getNextRangeDate(Date today, Date[] range) {
		if (today.before(range[1]) || compareDates(range[1], today))
			return false;

		Calendar cal = new GregorianCalendar();

		cal.setTime(range[0]);
		cal.add(Calendar.YEAR, 1);
		if (cal.getTime().after(today))
			cal = new GregorianCalendar(); // today
		Date newFrom = new GregorianCalendar(cal.get(Calendar.YEAR),
				cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0,
				1).getTime();

		cal.setTime(range[1]);
		cal.add(Calendar.YEAR, 1);
		if (cal.getTime().after(today))
			cal = new GregorianCalendar(); // today
		Date newEnd = new GregorianCalendar(cal.get(Calendar.YEAR),
				cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 23,
				59, 59).getTime();

		range[0] = newFrom;
		range[1] = newEnd;

		return true;
	}

	public static void getRangeDateSinceByDay(int y, int m, int d, Date[] range) {
		Date fromDate = new GregorianCalendar(y, m, d, 0, 0, 1).getTime();

		Calendar cal = new GregorianCalendar(y, m, d);
		cal.add(Calendar.DAY_OF_MONTH, 1);
		Date toDate = cal.getTime();

		range[0] = fromDate;
		range[1] = toDate;
	}

	public static boolean getNextRangeDateByDay(Date today, Date[] range) {
		if (compareDates(range[1], today))
			return false;

		Calendar cal = new GregorianCalendar();

		cal.setTime(range[0]);
		cal.add(Calendar.DAY_OF_MONTH, 1);
		if (cal.getTime().after(today))
			cal = new GregorianCalendar(); // today
		Date newFrom = cal.getTime();

		cal.setTime(range[1]);
		cal.add(Calendar.DAY_OF_MONTH, 1);
		if (cal.getTime().after(today))
			cal = new GregorianCalendar(); // today
		Date newEnd = cal.getTime();

		range[0] = newFrom;
		range[1] = newEnd;

		return true;
	}

	public static Date getTodayDate(Integer year) {
		Calendar cal = new GregorianCalendar();
		if (year != null) {
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, 11);
			cal.set(Calendar.DAY_OF_MONTH, 31);
		}
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		return cal.getTime();
	}

	/**
	 * I/O helper function
	 */

	public static void outputConsoleTitle(String title) {
		System.out.println("*******************************");
		System.out.println(title);
		System.out.println("*******************************");
	}

	public static void outputConsole(String msg) {
		System.out.println("\t " + msg);
	}

	public static void commentConsole(String msg) {
		System.out.println("# " + msg);
	}

	public static void printModel(int number, String modelPath, String vocPath,
			LinearModel model) throws Exception {
		// we want the results to display on the screen too,
		// that's why we put false as the last argument
		Results wordsResults = new Results(".", modelPath + ".words", false);
		model.wordCloudSetVocabulary(vocPath);

		System.out.println("=================================");
		wordsResults.println("Top " + number);
		printTopLeastWords(true, number, wordsResults, model);
		wordsResults.println("---------------------------------");
		wordsResults.println("Least " + number);
		printTopLeastWords(false, number, wordsResults, model);
		System.out.println("=================================");

		// close the according files
		wordsResults.Flush();
		wordsResults.SaveOutput();
	}

	public static void printTopLeastWords(boolean top, int number,
			Results wordsResults, LinearModel model) throws Exception {

		Map<String, Double> WordsCloud = top ? model
				.wordCloudGetTopFeatures(number) : model
				.wordCloudGetLeastFeatures(number);

		for (Map.Entry<String, Double> e : WordsCloud.entrySet()) {
			wordsResults.print(e.getKey());
			wordsResults.print(" : ");
			wordsResults.println(e.getValue().toString());
		}
	}

	/**
	 * Data point helper functions
	 */
	public static DataPoint buildDataPointTF(NGIndexer indexer,
			Object articleID, String articleContent) throws Exception {
		return buildDataPointTF(indexer, articleID, articleContent, null);
	}

	public static DataPoint buildDataPointTF(NGIndexer indexer,
			Object articleID, String articleContent, Integer label)
			throws Exception {
		Map<Integer, Double> features = indexer.CreateBOW_MapTF(articleContent);
		DataPoint sample = (label == null) ? new DataPoint(features)
				: new DataPoint(features, label);
		sample.setID(articleID);

		return sample;
	}

	public static DataPoint readDataPoint(BBDoc article) throws Exception {
		return readDataPoint(article, null);
	}

	public static DataPoint readDataPoint(BBDoc article, Integer label)
			throws Exception {
		List<Double> tfIdf = (List<Double>) article.getField("NYTndxTDC");
		if (tfIdf == null)
			return null;

		Map<Integer, Double> features = new TreeMap<Integer, Double>();
		int index;
		double value;
		for (int f = 0; f < tfIdf.size(); f += 2) {
			index = (int) Math.round(tfIdf.get(f));
			value = tfIdf.get(f + 1);
			features.put(index, value);
		}

		DataPoint sample = new DataPoint(features);
		sample.setID(article.getID());
		if (label != null)
			sample.setRealLabel(label);
		return sample;
	}

	public static DataPoint buildDataPoint(NGIndexer indexer, Object articleID,
			String articleContent) throws Exception {
		return buildDataPoint(indexer, articleID, articleContent, null);
	}

	public static DataPoint buildDataPoint(NGIndexer indexer, Object articleID,
			String articleContent, Integer label) throws Exception {
		Map<Integer, Double> features = indexer.CreateBOW_Map(articleContent);
		DataPoint sample = (label == null) ? new DataPoint(features)
				: new DataPoint(features, label);
		sample.setID(articleID);

		return sample;
	}

	public static DataPoint buildDataPointFromHashDb(BBDoc article,
			List<Double> hashedFeatures) {
		if (hashedFeatures == null)
			return null;

		Map<Integer, Double> hashedFeaturesMap = new HashMap<Integer, Double>();

		for (int i = 0; i < hashedFeatures.size(); i += 2) {
			Integer index = hashedFeatures.get(i).intValue();
			Double value = hashedFeatures.get(i + 1);
			hashedFeaturesMap.put(index, value);
		}
		DataPoint sample = new DataPoint(hashedFeaturesMap);
		sample.setID(article.getID());
		return sample;
	}

	public static DataPoint buildDataPointAndHash(Vocabulary voc,
			NGPreprocessing preProcess, Object articleID, String articleContent)
			throws Exception {
		return buildDataPointAndHash(voc, preProcess, articleID,
				articleContent, null);
	}

	public static DataPoint buildDataPointAndHash(Vocabulary voc,
			NGPreprocessing preProcess, Object articleID,
			String articleContent, Integer label) {
		String stemmedDocument = preProcess.doPreprocess(articleContent);

		DataPoint sample = voc.getDataPoint(stemmedDocument);
		if (label != null)
			sample.setRealLabel(label);
		sample.setID(articleID);

		return sample;
	}

	public static DataPoint buildDataPointAndHashTF(Vocabulary voc,
			NGPreprocessing preProcess, Object articleID, String articleContent)
			throws Exception {
		return buildDataPointAndHashTF(voc, preProcess, articleID,
				articleContent, null);
	}

	public static DataPoint buildDataPointAndHashTF(Vocabulary voc,
			NGPreprocessing preProcess, Object articleID,
			String articleContent, Integer label) {
		String stemmedDocument = preProcess.doPreprocess(articleContent);

		DataPoint sample = voc.getDataPointTF(stemmedDocument);
		if (label != null)
			sample.setRealLabel(label);
		sample.setID(articleID);
		return sample;
	}

	public static Double logistic(Double a) {
		if (a == null)
			return a;
		return 1 / (1 + Math.exp(-a));
	}

	public static Double extractFeatureValue(BBDoc article, String featureName) {

		if (featureName.equals("ReadTDC")) {
			BasicDBList readingInfos = (BasicDBList) article
					.getField("ReadTDC");
			if (readingInfos == null)
				return null;
			return (Double) readingInfos.get(0);
		}

		if (featureName.equals("Length")) {
			BasicDBList readingInfos = (BasicDBList) article
					.getField("ReadTDC");
			if (readingInfos == null)
				return null;
			Double noOfSentences = (Double) readingInfos.get(2);
			Double noOfwordsperSentence = (Double) readingInfos.get(3);
			return noOfSentences * noOfwordsperSentence;
		}
		return (Double) article.getField(featureName);
	}

	public static DataPoint buildDataPointFeatures(BBDoc article,
			List<String> featuresFieldnames) {

		Map<Integer, Double> coords = new TreeMap<Integer, Double>();
		int i = 1; // because BasicLinearModel stores the bias at index 0 all
					// representations start at index 1
		for (String feature : featuresFieldnames) {
			Double featureValue = extractFeatureValue(article, feature);
			featureValue = featureValue != null ? featureValue : 0.0;
			coords.put(i++, featureValue);
		}
		DataPoint sample = new DataPoint(coords);
		sample.setID(article.getID());

		return sample;
	}

	public static DataPoint buildDataPointRawText(BBDoc article) {
		String content = extractArticleContent(article);
		return content != null ? new DataPoint(content) : null;
	}

	private static Integer minDateIndex(List<Date> dates) {
		Date minDate = null;
		Integer pos = null;
		int i = 0;
		for (Date d : dates) {
			if (minDate == null)
				minDate = d;
			if (d != null && minDate != null && minDate.after(d)) {
				pos = i;
				minDate = d;
			}
			++i;
		}
		return pos;
	}

	public static void mergeDateSortedDatasets(List<DataPoint> data,
			Map<Date, List<DataPoint>> posData,
			Map<Date, List<DataPoint>> negData) {
		for (Date d : negData.keySet()) {
			if (posData.containsKey(d))
				posData.get(d).addAll(negData.get(d));
			else
				posData.put(d, negData.get(d));
		}
		for (Date d : posData.keySet()) {
			for (DataPoint sample : posData.get(d)) {
				data.add(sample);
			}
		}
	}

	public static void mergeShuffledDatasets(List<DataPoint> data,
			Map<Date, List<DataPoint>> posData,
			Map<Date, List<DataPoint>> negData, boolean balanceSet) {
		int minDatasetSize = Collections.min(Arrays.asList(posData.size(),
				negData.size()));
		for (Date d : posData.keySet())
			for (DataPoint sample : posData.get(d))
				data.add(sample);
		Collections.shuffle(data);

		if (balanceSet && posData.size() > minDatasetSize)
			for (int i = posData.size(); i > minDatasetSize; i--)
				data.remove(data.size() - 1);

		List<DataPoint> tmp = new ArrayList<DataPoint>();
		for (Date d : negData.keySet())
			for (DataPoint sample : negData.get(d))
				tmp.add(sample);
		Collections.shuffle(tmp);

		if (balanceSet && negData.size() > minDatasetSize)
			for (int i = negData.size(); i > minDatasetSize; i--)
				tmp.remove(tmp.size() - 1);
		data.addAll(tmp);
		Collections.shuffle(data);
	}

	/**
	 * DB helper functions
	 */

	public static String controlTagType(String tag) {

		if (0 == tag.indexOf("POST>"))
			return "POST";
		if (0 == tag.indexOf("FOR>"))
			return "FOR";
		return null;
	}

	public static String extractArticleContent(BBDoc article) {
		return article.getFieldString("T") + // article title
				". " + article.getFieldString("D") + // article description
				". " + article.getFieldString("C"); // article content
	}

	public static List<Double> getArticleContentHash(BBDoc article) {
		return (List<Double>) article.getField("h12tdc");
	}

	public static int registerTag(BlackBoardDateBased bb, String tagName)
			throws Exception {
		int tagId = bb.getTagID(tagName);
		if (tagId == BlackBoard.TAG_NOT_FOUND)
			tagId = bb.insertNewTag(tagName);
		return tagId;
	}

	public static void readTags(String commaSepratedTags,
			OnlineClassifier_StorageLayer storageLayer,
			Map<String, Integer> tags) throws Exception {
		String tagNames[] = commaSepratedTags.split(",");
		for (String tagName : tagNames) {

			if (tagName.equals(""))
				continue;

			int tagId = storageLayer.getInputTagID(tagName);
			// outputConsole(tagName + " => " + tagId);
			if (tagId == BlackBoard.TAG_NOT_FOUND)
				throw new Exception("Unknown input tag: " + tagName);
			tags.put(tagName, tagId);
		}
	}

	/**
	 * Module parametrization helper functions
	 * 
	 */

	// feature spaces
	public enum FeatureSpace {
		TF, TFIDF, HASH_TF, HASH_TFIDF, CONCEPTS, RANDOMIZED_VECTOR_DB, RAWTEXT
	};

	public enum SGDTransfertFunction {
		SIGN, LOGISTIC, LINEAR, HINGE
	}

	public enum SGDRegularizerFunction {
		NONE, L2, L1
	}

	public enum SGDLearningRateStrategy {
		CONSTANT, DECAYING, ADAPTIVE
	}

	public enum SGDLoss {
		PERCEPTRON, PERCEPTRON_L2, LOGISTIC_REGRESSION, LOGISTIC_REGRESSION_L2, LINEAR_REGRESSION, LINEAR_REGRESSION_L2, RIDGE, LINEAR_REGRESSION_L1, LASSO, HINGE_REGRESSION_L2, SVM, HINGE_REGRESSION_L1
	}

	// headers for perceptron/cpegasos perfs
	public final static String HEADER = "Precision \t " + "Recall \t "
			+ "F-measure \t " + "TP \t " + "FP \t " + "TN \t " + "FN \t "
			+ "Error \t " + "N_Pos \t " + "N_Neg \t " + "Ratio \t "
			+ "LearningFactor \t" + "Bias \t " + "l2norm\t ";

	// labels for classification/ranking

	public final static int LABEL_POS_CLASS = 1;
	public final static String LABEL_POS_CLASS_STRING = "POSITIVE";
	public final static int LABEL_NEG_CLASS = -1;
	public final static String LABEL_NEG_CLASS_STRING = "NEGATIVE";

	public enum BinaryLabel {
		POSITIVE(1), NEGATIVE(-1);

		public Integer numericValue;

		BinaryLabel(int numericValue) {
			this.numericValue = numericValue;
		}

		public static BinaryLabel fromInt(int v) {
			return v == 1 ? POSITIVE : NEGATIVE;
		}

	}

	public final static int POPULAR_TAG = 1;
	public final static int DISLIKED_TAG = -1;

	public final static Double NB_BYTES_FOR_HASH = 12.0;

	// feeds for classification

	public final static Integer BBC_SPORT_OTHER = 3790;
	public final static Integer BBC_SPORT_HOME = 3893;
	public final static Integer BBC_TOP_STORIES = 3813;
	public final static Integer BBC_ENTERTAINMENT = 78;
	public final static Integer BBC_POLITICS = 3818;
	public final static Integer BBC_TECHNOLOGY = 2117;
	public final static Integer BBC_EDUCATION = 3816;
	public final static Integer BBC_SCIENCE = 3815;


	// feeds for ranking
	public final static Integer BBC_HOME_PAGE = 9;
	public final static Integer NPR_HOME = 1306;
	public final static Integer SEATTLE_HOME = 1290;

	public enum InputFeedForRanking {
		BBC(Helpers.BBC_HOME_PAGE), NPR(Helpers.NPR_HOME), SEATTLE(
				Helpers.SEATTLE_HOME);

		public Integer id;

		InputFeedForRanking(int id) {
			this.id = id;
		}
	};

	// tags for modules
	public final static String TAG_CTRL_INPUT = "FOR>FBN_TESTING2012";
	public final static String TAG_CTRL_PROCESSING = "POST>FBN_TESTING2012";
	public final static String TAG_LABEL_SPORT = "FBN>SPORT2012";

	public enum ModuleMode {
		PREDEPLOYEMENT, PRODUCTION, EXPERIMENT;
	}

	public enum Months {
		JAN(0), FEB(1), MAR(2), APR(3), MAY(4), JUN(5), JUL(6), AUG(7), SEPT(8), OCT(
				9), NOV(10), DEC(11);

		public Integer value;

		Months(int value) {
			this.value = value;
		}
	};

	public static class Days {
		public int value;

		public Days(int value) throws Exception {
			if (value < 1 || value > 31)
				throw new Exception("Day " + value + " is out of range !");
			this.value = value;
		}
	}
}
