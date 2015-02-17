package macsy.lib.onlineLearning;

/**
 * This class implements the Clasher: This algorithm approximates the center of mass of a class with its exponential smoothing average. 
 * Unlike the algorithms provided in macsy.lib.onlineLearning.SGD.java it learns in a multiclass setting and trains generatively on a 
 * stream. It also has inherent parallel processing capabilities and under mild conditions has been proved to behave well under a compressed 
 * transformation of the original input space.
 * 
 * 
 * @author Fabon Dzoagang <dzogang.fabon@gmail.com>
 * @since 2015-01-01
 * 
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import macsy.lib.BasicLinearModel;
import macsy.lib.ConfusionMatrix;
import macsy.lib.DataPoint;
import macsy.lib.DoubleKey;
import macsy.lib.Helpers;
import macsy.lib.LRU3KCache;
import macsy.lib.LinearModel;
import macsy.lib.Pair;
import macsy.lib.RealHash;
import macsy.lib.Tessella;
import macsy.lib.onlineLearning.OnlineLearning;

public class Clasher implements OnlineLearning {
	public static final int PROCESSING_LIMIT = Integer.MAX_VALUE;
	public static final Double DECISION_THRESHOLD = 0.5;

	protected HashMap<String, List<Integer>> _mapper;
	protected List<Tessella> _regions;
	protected String _similarityFunction; // "l2distance", "cosine",
											// "inner_product"
	// protected double _minMargin = 0.01;
	protected int _initNumberPrototypesByClass = 1;
	protected LRU3KCache<DoubleKey, Double> _cacheIp;
	protected int _cacheSize = 3000;
	// protected int _minSupportToPredict = 1;
	protected Map<String, Integer> _indexesForLabels;
	Set<String> _allLabels;

	private Map<String, Integer> _mapConfusion;
	private List<ConfusionMatrix> _listConfusion;

	protected int _counter = 0;
	// protected int _counterUsed = 0;
	protected int _initializationLimit = 0;
	protected String _outFilename;

	protected HashMap<String, Integer> _nProtsByClass;

	private boolean _regionsFullyInitialized = false;

	private int expMovingAverage_window;
	private double expMovingAverage_a;
	private double expMovingAverage_error; // Micro error

	private int Nvalidation;
	private int validationTP;
	private int validationTN;
	private int validationFP;
	private int validationFN;
	private int validationNpos;
	private int validationNneg;
	private double expMovingAverage_ValidationError; // Micro error

	boolean _constantNormPrototypes = false;
	boolean _decayingRate = true;
	boolean _updateOnError = false;
	double _regularizationFactor = 1.0;

	public Clasher(Set<String> allLabels, int expMovingAverage_window)
			throws Exception {
		_indexesForLabels = new HashMap<String, Integer>();

		_regions = new ArrayList<Tessella>();
		_mapper = new HashMap<String, List<Integer>>();
		_similarityFunction = "l2distance";// ;"inner-product"
		_cacheIp = new LRU3KCache<DoubleKey, Double>(_cacheSize);

		set_mapConfusion(new HashMap<String, Integer>());
		set_listConfusion(new ArrayList<ConfusionMatrix>());

		_allLabels = new TreeSet<String>(allLabels);
		for (String label : _allLabels) {
			addLabel(label);
			get_mapConfusion().put(label, get_mapConfusion().size());
			get_listConfusion().add(new ConfusionMatrix());
			_mapper.put(label, null);
		}

		_nProtsByClass = new HashMap<String, Integer>();

		for (String label : _allLabels)
			_nProtsByClass.put(label, 0);

		validationTP = 1;
		validationTN = 1;
		validationFP = 1;
		validationFN = 1;
		validationNpos = 2;
		validationNneg = 2;
		Nvalidation = 4;
		expMovAvSetWindow(expMovingAverage_window);
	}

	public void setUpdateBias(boolean updateBias) {
		_constantNormPrototypes = !updateBias;
	}

	public void setDecayingRate(boolean decayingRate) {
		_decayingRate = decayingRate;
	}

	public void setUpdateOnError(boolean updateOnError) {
		_updateOnError = updateOnError;
	}

	public void setLamdaReg(double regularizationFactor) {
		_regularizationFactor = regularizationFactor;
	}

	public BasicLinearModel getDerivedLinearModel() throws Exception {
		RealHash popularPrototype = _regions.get(_mapper.get(
				Helpers.LABEL_POS_CLASS_STRING).get(0)).prototype;
		RealHash dislikedPrototype = _regions.get(_mapper.get(
				Helpers.LABEL_NEG_CLASS_STRING).get(0)).prototype;
		TreeMap<Integer, Double> posFeatures = popularPrototype.getHash();
		TreeMap<Integer, Double> negFeatures = dislikedPrototype.getHash();

		Set<Integer> allFeatures = new HashSet<Integer>(posFeatures.keySet());
		Set<Integer> remainingFeatures = new HashSet<Integer>(
				negFeatures.keySet());
		remainingFeatures.removeAll(posFeatures.keySet());
		allFeatures.addAll(remainingFeatures);
		Map<Integer, Double> w = new TreeMap<Integer, Double>();
		for (Integer featureIndex : allFeatures) {
			Double posValue = posFeatures.containsKey(featureIndex) ? posFeatures
					.get(featureIndex) : 0.0;
			Double negValue = negFeatures.containsKey(featureIndex) ? negFeatures
					.get(featureIndex) : 0.0;
			w.put(featureIndex, (posValue - negValue));
			// FIXME update the bias : (|p_+| - |p_-|) / 2
		}
		BasicLinearModel res = new BasicLinearModel(w, 0.0);
		return res;
	}

	public void saveWords(String wordFilePath) throws Exception {
		// FIXME put the bias : (|p_+| - |p_-|) / 2
		// linearModel.saveModel(wordFilePath);
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

	public void update_exponentialMovingAverageError(boolean error)
			throws Exception {
		expMovingAverage_error = expMovingAverage_a * (error ? 1 : 0)
				+ (1.0 - expMovingAverage_a) * expMovingAverage_error;
	}

	public void update_exponentialMovingAverageValidationError(boolean error)
			throws Exception {
		expMovingAverage_ValidationError = expMovingAverage_a * (error ? 1 : 0)
				+ (1.0 - expMovingAverage_a) * expMovingAverage_ValidationError;
	}

	@Override
	public double expMovAvGetError() {
		return expMovingAverage_error;
	}

	public void expMovAvSetError(double err) {
		expMovingAverage_error = err;
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
	}

	public int expMovAvGetWindow() {
		return expMovingAverage_window;
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

	@Override
	public void setLearningFactor(double learnFactor) {
		// TODO Auto-generated method stub
		// FIXME

	}

	@Override
	public double getLearningFactor() {
		// TODO Auto-generated method stub

		// FIXME
		return 0;
	}

	@Override
	public void setPosMargin(double margin) throws Exception {
		// TODO Auto-generated method stub

		throw new Exception(
				"The method setPosMargin is not meaningfull for the Clasher !");
		// USELESS ?
	}

	@Override
	public void setNegMargin(double margin) {
		// TODO Auto-generated method stub

		// USELESS ?

	}

	public String getLabelFromRegion(HashMap<String, Double> histogram) {
		double max = 0;
		String label = "";
		for (Map.Entry<String, Double> e : histogram.entrySet()) {
			if (e.getValue().doubleValue() > max) {
				max = e.getValue().doubleValue();
				label = e.getKey();
			}
		}
		return label;
	}

	public TreeMap<String, Double> predictHistogram(DataPoint x)
			throws Exception {

		RealHash doc_features = new RealHash(x); // shallow copy

		TreeMap<String, Double> histogram = new TreeMap<String, Double>();
		for (Tessella region : _regions) {
			Double d = region.getProtoNorm2() - 2.0
					* region.similarityDot(doc_features) + 1.0;
			String label = getLabelFromRegion(region.getHistogram());
			histogram.put(label, (double) 1 / (double) (1 + d));
		}

		return histogram;
	}

	private void loadLogFiles(String filePath,
			Map<String, Integer> nbProcessedSamples) throws Exception {

		BufferedReader inputLog = new BufferedReader(new FileReader(filePath));
		String line = null;
		while (((line = inputLog.readLine()) != null) && line.equals(""))
			/* nothing */;
		line = inputLog.readLine(); // skip header
		String toks[] = line.split("\t");
		expMovAvSetWindow(Integer.parseInt(toks[8]));
		expMovAvSetError(Double.parseDouble(toks[9]));

		/*
		 * Does not load the global log file (except for reading the expMovErr
		 * see above) since its values are taken from all detailed (per class)
		 * log files. When saving, the overall log is computed again from all
		 * restored and updated class details.
		 * 
		 * Supports for classes are loaded from the model file see loadModel
		 */

		for (String label : _allLabels) {
			BufferedReader inputLogDetailed = new BufferedReader(
					new FileReader(filePath + label));

			line = null;
			while (((line = inputLogDetailed.readLine()) != null)
					&& line.equals(""))
				/* nothing */;
			line = inputLogDetailed.readLine(); // skip header
			String toksDetailed[] = line.split("\t");

			ConfusionMatrix matrixDetailed = statsGetDetailedConfusionMatrix(label);

			matrixDetailed.setPositive_given_positive(Double
					.parseDouble(toksDetailed[2]));
			matrixDetailed.setNegative_given_negative(Double
					.parseDouble(toksDetailed[3]));
			matrixDetailed.setPositive_given_negative(Double
					.parseDouble(toksDetailed[4]));
			matrixDetailed.setNegative_given_positive(Double
					.parseDouble(toksDetailed[5]));

			System.out.println("nb read processed examples for " + label
					+ " IS " + Integer.parseInt(toksDetailed[6]));
			nbProcessedSamples.put(label, Integer.parseInt(toksDetailed[6]));

			inputLogDetailed.close();
		}
	}

	private void saveLogFiles(String filePath) throws Exception {

		Date curDate = new Date();
		long timestamp = curDate.getTime();

		BufferedWriter outputLog = new BufferedWriter(new FileWriter(filePath,
				false));
		String header = "Timestamp \t MacroError \t MicroError \t TP \t TN \t FP \t FN \t N_total \t ExpMicroWin \t ExpMicroErr\n";
		outputLog.write(header);
		ConfusionMatrix microMatrix = statsGetMicroMatrix();
		String line = Long.toString(timestamp) + "\t"
				+ (1 - statsGetMacroAccuracy()) + "\t"
				+ microMatrix.getClassificationError() + "\t"
				+ microMatrix.getPositive_given_positive() + "\t"
				+ microMatrix.getNegative_given_negative() + "\t"
				+ microMatrix.getPositive_given_negative() + "\t"
				+ microMatrix.getNegative_given_positive() + "\t" + statsGetN()
				+ "\t" + expMovAvGetWindow() + "\t" + expMovAvGetError() + "\n";
		outputLog.write(line);
		outputLog.close();

		for (String label : _allLabels) {
			BufferedWriter outputLogDetailed = new BufferedWriter(
					new FileWriter(filePath + label, false));
			String detailedHeader = "Timestamp \t error \t TP \t TN \t FP \t FN \t N\n";
			outputLogDetailed.write(detailedHeader);
			ConfusionMatrix matrixDetailed = statsGetDetailedConfusionMatrix(label);
			line = Long.toString(timestamp) + "\t"
					+ matrixDetailed.getClassificationError() + "\t"
					+ matrixDetailed.getPositive_given_positive() + "\t"
					+ matrixDetailed.getNegative_given_negative() + "\t"
					+ matrixDetailed.getPositive_given_negative() + "\t"
					+ matrixDetailed.getNegative_given_positive() + "\t"
					+ statsGetSupport(label) + "\n";
			outputLogDetailed.write(line);
			outputLogDetailed.close();
		}
	}

	private void saveRegionsSummary(BufferedWriter out) throws Exception {
		for (Map.Entry<String, List<Integer>> topicMapEntry : _mapper
				.entrySet())
			for (Integer position : topicMapEntry.getValue()) {
				out.write("REGION:" + topicMapEntry.getKey() + ":" + position
						+ "\n");
				Helpers.outputConsole("REGION:" + topicMapEntry.getKey() + ":"
						+ topicMapEntry.getValue());
			}
	}

	private void saveRegionsPrototypes(BufferedWriter out) throws Exception {
		for (Map.Entry<String, List<Integer>> topicMapEntry : _mapper
				.entrySet())
			for (Integer position : topicMapEntry.getValue()) {

				Tessella region = _regions.get(position);
				double n = region.getSupport();
				RealHash prototype = region.prototype;
				out.write("PROTOTYPE:" + topicMapEntry.getKey() + ":"
						+ position + ":" + n + "\n");
				Helpers.outputConsole("PROTOTYPE:" + topicMapEntry.getKey()
						+ ":" + position + ":" + n + " l2norm ["
						+ prototype.norm() + "]");
				for (Map.Entry<Integer, Double> dimensions : prototype
						.getHash().entrySet())
					out.write("DIM:" + dimensions.getKey() + ":"
							+ dimensions.getValue() + "\n");
				out.write("\n");
				out.write("HISTOGRAM:" + "\n");
				for (Map.Entry<String, Double> histEntry : region
						.getLabelsHistogram().entrySet())
					out.write("BIN:" + histEntry.getKey() + ":"
							+ histEntry.getValue() + "\n");
				out.write("\n");
			}
	}

	@Override
	public void saveModel(String filename) throws IOException {
		try {

			saveLogFiles(filename + ".log");

			BufferedWriter outModel = new BufferedWriter(new FileWriter(
					filename, false));
			saveRegionsSummary(outModel);
			Helpers.outputConsole("");
			saveRegionsPrototypes(outModel);
			outModel.close();
		} catch (Exception e) {
			System.out.println("Error saving model ");
			e.printStackTrace();
		}
	}

	@Override
	public void loadModel(String filename) {
		try {

			Map<String, Integer> nbProcessedSamples = new HashMap<String, Integer>();
			loadLogFiles(filename + ".log", nbProcessedSamples);

			BufferedReader source_ = new BufferedReader(
					new FileReader(filename));
			String line;
			int n_topics = 0;

			while ((line = source_.readLine()) != null) {
				while ((line != null) && !line.contains("REGION:"))
					line = source_.readLine();
				if (line == null)
					break;

				String[] things_in_line1 = line.split(":");
				String label = things_in_line1[1].trim();
				Integer position = Integer.parseInt(things_in_line1[2].trim());
				List<Integer> list_positions = _mapper.get(label);
				if (list_positions == null)
					list_positions = new ArrayList<Integer>();
				list_positions.add(position);
				this._mapper.put(label, list_positions);

				System.out.println("Reading Labels "
						+ things_in_line1[1].trim() + " New position:"
						+ position);

				n_topics++;
			}

			for (int n = 0; n < n_topics; n++)
				_regions.add(new Tessella(_regions.size()));

			source_.close();

			Integer N = 0;
			BufferedReader source = new BufferedReader(new FileReader(filename));
			while ((line = source.readLine()) != null) {
				System.out.println("Reading Regions ...");

				while ((line != null) && !line.contains("PROTOTYPE:"))
					line = source.readLine();

				if (line == null)
					break;

				String[] things_in_line = line.split(":");
				String label = things_in_line[1].trim();
				Integer position = Integer.parseInt(things_in_line[2].trim());
				Double support = Double.parseDouble(things_in_line[3].trim());
				N += support.intValue();
				if (position == null)
					System.out.println("no no no");

				Tessella region = _regions.get(position);
				region.setClass(label);
				region.setSeen(nbProcessedSamples.get(label));
				System.out.println(nbProcessedSamples.get(label)
						+ " processed examples for label [" + label + "]");
				if (!_nProtsByClass.containsKey(label))
					_nProtsByClass.put(label, 0);
				_nProtsByClass.put(label, _nProtsByClass.get(label) + 1);

				System.out.println("Prototype:" + label + " Position: "
						+ position + " Support:" + support);
				region.setSupport(support);

				line = source.readLine();

				while (line.contains("DIM:")) {
					things_in_line = line.split(":");
					region.prototype.setDimValue(
							Integer.parseInt(things_in_line[1].trim()),
							Double.parseDouble(things_in_line[2].trim()));
					line = source.readLine();
				}

				while ((line != null) && !line.contains("HISTOGRAM:"))
					line = source.readLine();

				line = source.readLine();

				while (line.contains("BIN:")) {
					things_in_line = line.split(":");
					region.getLabelsHistogram().put(things_in_line[1].trim(),
							Double.parseDouble(things_in_line[2].trim()));
					line = source.readLine();
				}
			}
			incrementN_overall(N);
			source.close();
			_regionsFullyInitialized = true;

		} catch (Exception e) {
			System.out.println("Error loading model " + e.getMessage());
		}

	}

	// get prototype norm
	public Double statsGetPrototypeL2Norm(String label) {
		return _regions.get(_mapper.get(label).get(0)).prototype.norm();
	}

	// get support for each class
	public Integer statsGetSupport(String label) {
		Integer support = 0;

		for (Integer position : _mapper.get(label))
			support += _regions.get(position).getSupport().intValue();
		return support;
	}

	// get detailed confusion matrix
	public ConfusionMatrix statsGetDetailedConfusionMatrix(String label) {
		return _listConfusion.get(_mapConfusion.get(label));
	}

	// get overall micro Matrix
	public ConfusionMatrix statsGetMicroMatrix() {
		return ConfusionMatrix.microMatrix(_listConfusion);
	}

	// Macro F score
	public Double statsGetMacroFscore() {
		return ConfusionMatrix.macroAverage(_listConfusion);
	}

	// Macro Accuracy
	public Double statsGetMacroAccuracy() {
		return ConfusionMatrix.macroAccuracy(_listConfusion);
	}

	// Macro Precision
	public Double statsGetMacroPrecision() {
		return ConfusionMatrix.macroPrecision(_listConfusion);
	}

	// Macro Recall
	public Double statsGetMacroRecall() {
		return ConfusionMatrix.macroRecall(_listConfusion);
	}

	// Micro Fscore
	public Double statsGetMicroFscore() {
		return ConfusionMatrix.microAverage(_listConfusion);
	}

	// Micro Accuracy
	public Double statsGetMicroAccuracy() {
		return statsGetMicroAccuracy(null);
	}

	public Double statsGetMicroAccuracy(ConfusionMatrix microMatrix) {
		microMatrix = (microMatrix == null) ? statsGetMicroMatrix()
				: microMatrix;
		return 1.0 - microMatrix.getClassificationError();
	}

	// Micro Precision
	public Double statsGetMicroPrecision() {
		return statsGetMicroPrecision(null);
	}

	public Double statsGetMicroPrecision(ConfusionMatrix microMatrix) {
		microMatrix = (microMatrix == null) ? statsGetMicroMatrix()
				: microMatrix;

		return microMatrix.getPrecision();
	}

	// Micro Recall
	public Double statsGetMicroRecall() {
		return statsGetMicroRecall(null);
	}

	public Double statsGetMicroRecall(ConfusionMatrix microMatrix) {
		microMatrix = (microMatrix == null) ? statsGetMicroMatrix()
				: microMatrix;

		return microMatrix.getRecall();
	}

	@Override
	public LinearModel getLinearModel() {
		// TODO Auto-generated method stub

		// USELESS ?
		return null;
	}

	@Override
	public void setDesiredPrecision(double precision) {
		// TODO Auto-generated method stub

		// USELESS ?
	}

	@Override
	public double getDesiredPrecision() {
		// TODO Auto-generated method stub

		// USELESS ?
		return 0;
	}

	@Override
	public double getDecisionThreshold() {
		// TODO Auto-generated method stub

		// USELESS ?
		return 0;
	}

	@Override
	public void setDecisionThreshold(double threshold) throws Exception {
		// TODO Auto-generated method stub

		// USELESS ?

	}

	@Override
	public long statsGetN() {
		return _counter;
	}

	@Override
	public void statsReset() {
		// TODO Auto-generated method stub

		// FIXME (re-use code for perceptron ?)

	}

	@Override
	public long statsGetTP() {
		// TODO Auto-generated method stub

		// FIXME (re-use code for perceptron ?)
		return 0;
	}

	@Override
	public long statsGetFP() {
		// TODO Auto-generated method stub

		// FIXME (re-use code for perceptron ?)
		return 0;
	}

	@Override
	public long statsGetFN() {
		// TODO Auto-generated method stub

		// FIXME (re-use code for perceptron ?)
		return 0;
	}

	@Override
	public long statsGetTN() {
		// TODO Auto-generated method stub

		// FIXME (re-use code for perceptron ?)
		return 0;
	}

	@Override
	public void statsPrintConfusionMatrix() throws Exception {
		// TODO Auto-generated method stub

		// FIXME (re-use code for perceptron ?)

	}

	@Override
	public double statsGetPrecision() throws Exception {
		// TODO Auto-generated method stub

		// FIXME (re-use code for perceptron ?)
		return 0;
	}

	@Override
	public double statsGetRecall() throws Exception {
		// TODO Auto-generated method stub

		// FIXME (re-use code for perceptron ?)
		return 0;
	}

	@Override
	public double expMovAvGetPositives() throws Exception {
		// TODO Auto-generated method stub

		// FIXME (re-use code for perceptron ?)
		return 0;
	}

	@Override
	public double expMovAvGetNegatives() throws Exception {
		// TODO Auto-generated method stub

		// FIXME (re-use code for perceptron ?)
		return 0;
	}

	@Override
	public void updateNum(DataPoint X) throws Exception {
		// TODO Auto-generated method stub

		// FIXME (re-use code for perceptron ?)

	}

	@Override
	public double getBias() {
		// TODO Auto-generated method stub

		// USELESS ?
		return 0;
	}

	@Override
	public boolean getupdateLearningFactor() {
		// TODO Auto-generated method stub

		// USELESS ?
		return false;
	}

	@Override
	public void setUpdateLearningFactor(boolean value) {
		// TODO Auto-generated method stub

		// USELESS ?

	}

	@Override
	public double getAUC() {
		// TODO Auto-generated method stub

		// FIXME (re-use code for perceptron ?)
		return 0;
	}

	@Override
	public void incrementN_overall(int addNumber) {
		_counter += addNumber;
	}

	// Returns the most similar and the dot product with this prototype
	public Pair<Tessella, Double> searchMostSimilar(RealHash documentHash) {
		return searchMostSimilar(null, documentHash, null, null);
	}

	public Pair<Tessella, Double> searchMostSimilar(Integer index,
			RealHash documentHash) {
		return searchMostSimilar(index, documentHash, null, null);
	}

	public Pair<Tessella, Double> searchMostSimilar(RealHash documentHash,
			Set<String> from, Set<String> notFrom) {
		return searchMostSimilar(null, documentHash, from, notFrom);
	}

	// Returns the most similar and the dot product with this prototype
	// if 'from' is not null, searches only among the prototypes named with
	// class 'from'
	// if 'notFrom' is not null, searches only among the prototypes named
	// different than class 'notFrom'
	public Pair<Tessella, Double> searchMostSimilar(Integer index,
			RealHash documentHash, Set<String> from, Set<String> notFrom) {

		Tessella decision = null;
		double largest_similarity = -Double.MIN_VALUE;
		Double dot_product = 0.0;
		double similarity = 0.0;

		for (Tessella region : _regions) {

			if ((from != null) && (!from.contains(region.getMainClass())))
				continue;// exclude this region

			if ((notFrom != null) && (notFrom.contains(region.getMainClass())))
				continue;// exclude this region

			if (index != null)
				dot_product = _cacheIp
						.get(new DoubleKey(index, region.getID()));

			if ((index == null) || (dot_product == null))
				dot_product = new Double(region.similarityDot(documentHash));

			if ((index != null) && (dot_product != null))
				_cacheIp.put(new DoubleKey(index, region.getID()), dot_product);

			similarity = dot_product;// /Math.sqrt(region.getProtoNorm2())

			if (similarity > largest_similarity) {
				decision = region;
				largest_similarity = similarity;
			}
		}

		return new Pair<Tessella, Double>(decision, largest_similarity);
	}

	public Pair<Tessella, Double> searchMostSimilarFromIndexes(Integer index,
			RealHash documentHash, List<Integer> fromIndexes) {

		Tessella decision = null;
		double largest_similarity = -Double.MAX_VALUE;
		Double dot_product = 0.0;
		double similarity = 0.0;

		for (Integer position : fromIndexes) {
			Tessella region = _regions.get(position);

			if (index != null)
				dot_product = _cacheIp
						.get(new DoubleKey(index, region.getID()));

			if ((index == null) || (dot_product == null))
				dot_product = new Double(region.similarityDot(documentHash));

			if ((index != null) && (dot_product != null))
				_cacheIp.put(new DoubleKey(index, region.getID()), dot_product);

			similarity = dot_product;// /Math.sqrt(region.getProtoNorm2())

			if (similarity > largest_similarity) {
				decision = region;
				largest_similarity = similarity;
			}
		}

		return new Pair<Tessella, Double>(decision, largest_similarity);
	}

	public Pair<Tessella, Double> searchNearest(RealHash documentHash) {
		return searchNearest(null, documentHash, null, null);
	}

	public Pair<Tessella, Double> searchNearest(Integer index,
			RealHash documentHash) {
		return searchNearest(index, documentHash, null, null);
	}

	public Pair<Tessella, Double> searchNearest(RealHash documentHash,
			Set<String> from, Set<String> notFrom) {
		return searchNearest(null, documentHash, from, notFrom);
	}

	
	/**
	 * Returns the nearest prototype and the distance with this prototype if 'from' is not null, 
	 * searches only among the prototypes named with class 'from' if 'notFrom' is not null, 
	 * searches only among the prototypes named different than class 'notFrom'
	 * @param index
	 * @param documentHash
	 * @param N
	 * @return list of (regions, similarity score) ranked by similarity score
	 */
	public List<Pair<Tessella, Double>> search_N_Nearest(Integer index,
			RealHash documentHash, int N) {
		if (N == 0)
			return null;

		List<Pair<Tessella, Double>> N_nearest = new ArrayList<Pair<Tessella, Double>>();
		Set<String> notFrom = new HashSet<String>();
		do {
			Pair<Tessella, Double> region = searchNearest(null, documentHash,
					null, notFrom);
			N_nearest.add(region);
			notFrom.add(region.getFirst().getMainClass());
		} while (--N > 0);

		return N_nearest;
	}

	public Pair<Tessella, Double> searchNearest(Integer index,
			RealHash documentHash, Set<String> from, Set<String> notFrom) {
		Tessella decision = null;
		Double nearest_distance = Double.MAX_VALUE;
		Double dot_product = 0.0;
		// double dot_product_nearest = 0.0;
		double distance = 0.0;

		for (Tessella region : _regions) {

			if ((from != null) && (!from.contains(region.getMainClass())))
				continue;
			if ((notFrom != null) && (notFrom.contains(region.getMainClass())))
				continue;

			// efficient computation of distances

			if (index != null)
				dot_product = _cacheIp
						.get(new DoubleKey(index, region.getID()));

			if ((index == null) || (dot_product == null))
				dot_product = new Double(region.similarityDot(documentHash));

			if ((index != null) && (dot_product != null))
				_cacheIp.put(new DoubleKey(index, region.getID()), dot_product);

			distance = region.getProtoNorm2() - 2.0 * dot_product + 1.0;
			// if (nearest_distance > distance) {
			if (nearest_distance > distance || decision == null) { // FIXME
																	// ensures
																	// at least
																	// one
																	// affectation
				decision = region;
				nearest_distance = distance;
				// dot_product_nearest = dot_product;
			}
		}

		return new Pair<Tessella, Double>(decision, nearest_distance);
	}

	public Pair<Tessella, Double> searchNearestFromIndexes(Integer index,
			RealHash documentHash, List<Integer> fromIndexes) {

		Tessella decision = null;
		Double nearest_distance = Double.MAX_VALUE;
		Double dot_product = 0.0;
		// double dot_product_nearest = 0.0;
		double distance = 0.0;

		for (Integer position : fromIndexes) {
			Tessella region = _regions.get(position);

			if (index != null)
				dot_product = _cacheIp
						.get(new DoubleKey(index, region.getID()));

			if ((index == null) || (dot_product == null))
				dot_product = new Double(region.similarityDot(documentHash));

			if ((index != null) && (dot_product != null))
				_cacheIp.put(new DoubleKey(index, region.getID()), dot_product);

			distance = region.getProtoNorm2() - 2.0 * dot_product + 1.0;

			/*
			 * direct computation of distance distance =
			 * region.distance(documentHash);
			 */

			// if (nearest_distance > distance || decision == null) {
			if (nearest_distance > distance || decision == null) { // FIXME
																	// enforce
																	// at least
																	// one
																	// affectation
				decision = region;
				nearest_distance = distance;
				// dot_product_nearest = dot_product;

			}
		}

		return new Pair<Tessella, Double>(decision, nearest_distance);
	}

	public void parseMultiLabel_SPARSE_LABELS(String _line,
			Set<String> _labels, RealHash _features) {

		String[] things_in_line = _line.split(",");
		String[] labels_line = things_in_line[0].trim().split("\\s+");

		for (int i = 0; i < labels_line.length; i++) {
			if (labels_line[i].trim().length() > 1)
				_labels.add(labels_line[i].trim());
		}

		String featureStr = things_in_line[things_in_line.length - 1].trim();
		String[] featureVals = featureStr.split("\\s+");

		int num_dims = featureVals.length;
		for (int i = 0; i < num_dims; i++) {
			Integer index = Integer
					.parseInt(featureVals[i].trim().split(":")[0]);
			Double value = Double
					.parseDouble(featureVals[i].trim().split(":")[1]);
			if ((value > 0.0) || (value < 0.0))
				_features.setDimValue((index - 1) / 10, value);
		}
	}

	// if the module is re-launched then the index (for the cache Ip) will be
	// overwritten which is consistent with the principle of caching on every
	// launch
	public void addByTags(Integer index, RealHash documentHash,
			Set<String> labels) {
		for (String label : labels) {// add to the nearest prototype
										// of each contained topic

			List<Integer> position_list = _mapper.get(label);
			if (position_list == null)
				continue;

			Pair<Tessella, Double> nearest_and_distance = searchNearestFromIndexes(
					index, documentHash, position_list);
			Tessella nearestPrototype = nearest_and_distance.getFirst();

			double dot_product_nearest = -1.0
					* (nearest_and_distance.getSecond() - 1.0 - nearestPrototype
							.getProto_norm_SQUARED()) / 2.0;

			nearestPrototype.setDecayingRate(_decayingRate);
			nearestPrototype.setConstantNorm(_constantNormPrototypes);
			nearestPrototype.addLabelledPoint(documentHash, labels,
					dot_product_nearest);

		}
	}

	protected boolean initPrototypes(RealHash doc_features, Set<String> labels)
			throws Exception {
		set_regionsFullyInitialized(true);
		for (String label : _allLabels)
			set_regionsFullyInitialized(is_regionsFullyInitialized()
					&& (_nProtsByClass.get(label) >= _initNumberPrototypesByClass));
		if (is_regionsFullyInitialized())
			return true;

		for (String label : labels) {
			if ((_nProtsByClass.get(label) < _initNumberPrototypesByClass)) {

				// use the point to initialize a centroid of this class
				List<Integer> listPositions = _mapper.get(label);
				if (listPositions == null)
					listPositions = new ArrayList<Integer>();
				listPositions.add(_regions.size());
				_mapper.put(label, listPositions);

				Tessella region = new Tessella(label, _regions.size());
				region.addLabelledPoint(doc_features, labels);
				_regions.add(region);
				_nProtsByClass.put(label, _nProtsByClass.get(label) + 1);

				System.out.println("New prototype for label:  " + label
						+ ", nb prototypes: " + _nProtsByClass.get(label)
						+ ", read data: " + _counter);

				break; // A sample point is used to initialise one
						// prototype/class only
			}
		}

		Helpers.outputConsole("[" + _counter + "] examples read");

		String initStateString = is_regionsFullyInitialized() ? " => [ Fully initialized ] "
				: "";
		Helpers.outputConsole("SIZE REGIONS: " + _regions.size()
				+ initStateString);

		Helpers.outputConsole("REGIONS: " + _regions.size() + "/"
				+ (_initNumberPrototypesByClass * _allLabels.size()));

		for (String label : _allLabels) {
			Helpers.outputConsole("Label :" + label + " nb prototypes :"
					+ _nProtsByClass.get(label));
		}

		return is_regionsFullyInitialized();
	}

	public void addLabel(String label) {
		_indexesForLabels.put(label, _indexesForLabels.size() + 1);
	}

	public boolean showInformation(int counter, int used) {
		System.out.println(counter + " examples read, " + used + " used.");
		// System.out.println(counter+" ..... training tesselation");
		System.out.println("   ..... topics found: " + _regions.size());
		// for(String regionname: mapper.keySet())
		// System.out.print(regionname+" ");
		System.out.println("");
		return true;
	}

	public void writeTime(String filename_time, int counter, double time)
			throws Exception {
		if (filename_time != null) {

			BufferedWriter out = new BufferedWriter(new FileWriter(
					filename_time, true));
			out.write("" + counter + " " + time + "\n");
			out.close();
		}
	}

	public void printPrototypesInfos() {
		for (Map.Entry<String, List<Integer>> topicMapEntry : _mapper
				.entrySet()) {
			for (Integer position : topicMapEntry.getValue()) {
				String label = topicMapEntry.getKey();
				Tessella region = _regions.get(position);
				double n = region.getSupport();
				RealHash prototype = region.prototype;
				Helpers.outputConsole("==" + label + " support [" + n
						+ "] l2norm [" + prototype.norm() + "]");
			}
		}
	}

	@Override
	public void train(DataPoint x) throws Exception {
		throw new Exception(
				"Multiclass classification, provide set of labels with input sample !");
	}

	public void train(DataPoint x, Set<String> labels) throws Exception {

		if (labels.size() <= 0)
			throw new Exception(
					"training examples must be associated with at least 1 label !");

		RealHash doc_features = new RealHash(x); // shallow copy

		if (!initPrototypes(doc_features, labels))
			return; // use input samples to initialize regions

		/**
		 * If regions are all initialised, then use input samples for training
		 * the clasher
		 */
		Helpers.outputConsole("Learning the Clasher model ...");
		// parseMultiLabel_SPARSE_LABELS(line, labels, doc_features);

		Set<String> predictedLabels = this.tag(_counter, doc_features);

		// String pred = predictedLabels.iterator().next();
		// String actual = labels.iterator().next();
		// System.out.println("Real Label : " + actual + " Predicted : " +
		// pred);

		boolean error = false;
		for (String label : _allLabels) {
			ConfusionMatrix m = get_listConfusion().get(
					get_mapConfusion().get(label));
			boolean predicted = predictedLabels.contains(label);
			boolean contained = labels.contains(label);
			m.addDecision(predicted, contained);
			error = error || (predicted != contained);
		}

		// calculate the error rate (exponential moving error average)
		update_exponentialMovingAverageError(error);

		if (!_updateOnError)
			addByTags(_counter, doc_features, labels);
		else if (error)
			addByTags(_counter, doc_features, labels);

	}

	public Tessella getAssignedRegion(Integer index, RealHash documentHash) {
		Tessella assignedRegion = null;

		// if (this._similarityFunction.compareTo("l2distance") == 0)
		assignedRegion = searchNearest(index, documentHash).getFirst();
		// else
		// assignedRegion = searchMostSimilar(index, documentHash).getFirst();

		return assignedRegion;
	}

	public TreeMap<String, Double> getProbsFromRegion(Tessella region) {
		TreeMap<String, Double> histogram = new TreeMap<String, Double>(
				region.getHistogram()); // deep copy
		for (Map.Entry<String, Double> topicEntry : histogram.entrySet()) {
			double probability = topicEntry.getValue().doubleValue()
					/ region.getSupport().doubleValue();
			topicEntry.setValue(probability);
		}
		return histogram;
	}

	public Set<String> tag(Integer index, RealHash documentHash)
			throws Exception {
		Tessella assignedRegion = getAssignedRegion(index, documentHash);
		TreeMap<String, Double> histogram = getProbsFromRegion(assignedRegion);

		Set<String> set_contained_topics = new HashSet<String>();
		for (String label : histogram.keySet())
			if (histogram.get(label) > DECISION_THRESHOLD)
				set_contained_topics.add(label);

		return set_contained_topics;
	}

	public List<ConfusionMatrix> get_listConfusion() {
		return _listConfusion;
	}

	public void set_listConfusion(List<ConfusionMatrix> _listConfusion) {
		this._listConfusion = _listConfusion;
	}

	public Map<String, Integer> get_mapConfusion() {
		return _mapConfusion;
	}

	public void set_mapConfusion(Map<String, Integer> _mapConfusion) {
		this._mapConfusion = _mapConfusion;
	}

	public boolean is_regionsFullyInitialized() {
		return _regionsFullyInitialized;
	}

	public void set_regionsFullyInitialized(boolean _regionsFullyInitialized) {
		this._regionsFullyInitialized = _regionsFullyInitialized;
	}

	@Override
	public double predict(DataPoint x) throws Exception {
		throw new Exception(
				"Use predictHistogram instead for multiclass prediction !");
	}

	@Override
	public void writeHeader(String filename) throws IOException {
		// TODO Auto-generated method stub

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

	public void update_validationError(TreeMap<String, Double> histogram,
			int trueLabel) throws Exception {
		++Nvalidation;
		validationNpos += trueLabel == 1 ? 1 : 0;
		validationNneg += trueLabel == -1 ? 1 : 0;
		boolean popularPrediction = histogram
				.get(Helpers.LABEL_POS_CLASS_STRING) > histogram
				.get(Helpers.LABEL_NEG_CLASS_STRING);

		validationTP += popularPrediction && trueLabel == 1 ? 1 : 0;
		validationFP += popularPrediction && trueLabel == -1 ? 1 : 0;
		validationTN += !popularPrediction && trueLabel == -1 ? 1 : 0;
		validationFN += !popularPrediction && trueLabel == 1 ? 1 : 0;

		boolean error = popularPrediction && trueLabel == -1
				|| !popularPrediction && trueLabel == 1;
		update_exponentialMovingAverageValidationError(error);
	}

	@Override
	public void update_validationError(double prediction, int realLabel)
			throws Exception {
		// TODO Auto-generated method stub

	}
}
