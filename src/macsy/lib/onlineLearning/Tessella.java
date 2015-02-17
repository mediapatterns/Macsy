package macsy.lib.onlineLearning;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import macsy.lib.RealHash;

public class Tessella {

	public RealHash prototype;
	private HashMap<String, Double> labelsHistogram;
	private Double support;
	Integer seen;
	Integer hits;
	double sumVals2;

	String mainClass;

	// caching some values
	private Double proto_norm_SQUARED;
	boolean proto_norm_SQUARED_updated;
	boolean caching_while_training = false;
	public boolean proactiveNormComputation = true;
	int id;

	Double radius;

	Double pp;
	Double pn;
	Double np;
	Double nn;
	 
	 
	boolean _decayingRate = true;
	boolean _constantNormPrototypes = false;
	
	
	public void setDecayingRate(boolean decayingRate) {
		_decayingRate = decayingRate;
	}
	public void setConstantNorm(boolean constantNormPrototypes) {
		_constantNormPrototypes = constantNormPrototypes;
	}

	public Tessella(String _class, RealHash _init, int _id) {

		if (_init == null)
			prototype = new RealHash();
		else
			prototype = _init;

		setLabelsHistogram(new HashMap<String, Double>());
		sumVals2 = 0.0;

		setSupport(0.0);
		seen = 0;
		setProto_norm_SQUARED(0.0);
		proto_norm_SQUARED_updated = false;
		mainClass = new String(_class);
		this.id = _id;

		this.pp = 0.0;
		this.pn = 0.0;
		this.np = 0.0;
		this.nn = 0.0;
		this.hits = 0;
	}

	public Tessella(int id) {
		this("n.n.", null, id);
	}

	public Tessella(String _class) {
		this(_class, null, -1);
	}

	public Tessella(String _class, RealHash _init) {
		this(_class, _init, -1);
	}

	public Tessella(String _class, int id) {
		this(_class);
		this.id = id;
	}

	public void setClass(String _class) {
		mainClass = new String(_class);
	}
	
	public void setSeen(Integer seen) {
		this.seen = seen;
	}

	public String getMainClass() {
		return mainClass;
	}

	public int getID() {
		return id;
	}

	public double distance(RealHash point) {
		System.out.println("distance from scratch");
		return prototype.distancel2(point);
	}

	public double distanceOutput(Set<String> topics) {

		double dotHistograms = 0.0;
		double histNorm = Math.sqrt(sumVals2) / this.getSupport();

		for (Map.Entry<String, Double> e : getLabelsHistogram().entrySet()) {
			if (topics.contains(e.getKey()))
				dotHistograms += e.getValue();
		}
		dotHistograms = dotHistograms / (this.getSupport() * topics.size());

		double distanceO = (Math.sqrt(topics.size()) / topics.size())
				+ histNorm - 2.0 * dotHistograms;

		return distanceO;
	}

	public double similarityDot(RealHash point) {
		return prototype.dot(point);
	}

	public double similarityCosine(RealHash point) {
		return prototype.cosine(point);
	}

	// get the norm^2 of the prototype of this region
	public double getProtoNorm2() {
		if (!proto_norm_SQUARED_updated) {
			setProto_norm_SQUARED(prototype.norm2());
			System.out.println("computing proto.norm");
			proto_norm_SQUARED_updated = true;
		}
		return getProto_norm_SQUARED();
	}

	public double getOutputSimilarity(Set<String> contained_labels) {

		Double temp;
		Double output_similarity = 0.0;
		for (String label : contained_labels) {
			temp = getLabelsHistogram().get(label);
			if (temp != null)
				output_similarity += temp;
		}

		return output_similarity / contained_labels.size();

	}

	// assumes normalized point i.e. |x|=1
	public double similarityCosineEfficient(RealHash point) {
		if (!proto_norm_SQUARED_updated) {
			setProto_norm_SQUARED(prototype.norm2());
			proto_norm_SQUARED_updated = true;
		}
		return prototype.dot(point) / Math.sqrt(getProto_norm_SQUARED());
	}

	// assumes normalized point i.e. |x|=1
	public double distanceEfficient(RealHash point) {
		if (!proto_norm_SQUARED_updated) {
			setProto_norm_SQUARED(prototype.norm2());
			proto_norm_SQUARED_updated = true;
		}
		return (1.0 + getProto_norm_SQUARED() - (2 * prototype.dot(point)));
	}

	public HashMap<String, Double> getHistogram() {
		return getLabelsHistogram();

	}

	public void addDecision(boolean decision, boolean label) {

		if (decision && label)
			this.pp += 1.0;
		if (decision && !label)
			this.pn += 1.0;
		if (!decision && label)
			this.np += 1.0;
		if (!decision && !label)
			this.nn += 1.0;

	}

	public void weightCurrent(double lambda) {
		this.pp = (1.0 - lambda) * this.pp;
		this.pn = (1.0 - lambda) * this.pn;
		this.np = (1.0 - lambda) * this.np;
		this.nn = (1.0 - lambda) * this.nn;
	}

	public double getRecall() {// sensitivity
		if ((pp + np) > 0.0)
			return pp / (pp + np);
		return 1.0;
	}

	public double getPrecision() {
		if ((pp + pn) > 0.0)
			return pp / (pp + pn);
		return 1.0;
	}

	public double getF() {
		if (((2.0 * pp) + pn + np) > 0.0)
			return (2.0 * pp) / ((2.0 * pp) + pn + np);
		return 1.0;
	}

	static double weight_prev(double n) {
		return (n / (n + 1.0)); // 1 - (\lamda^t_i) in paper with \lamda^t_i =
								// 1/(n^t_i + 1)
	}

	static double weight_new(double n) {
		return (1.0 / (n + 1.0)); // \lambda^t_i in paper
	}
	
	public void addLabelledPoint(RealHash point, Set<String> contained_labels) {
		addPoint(point, contained_labels, null, null, null);
	}

	public void addLabelledPoint(RealHash point, Set<String> contained_labels,
			Double cosine_with_prototype) {
		addPoint(point, contained_labels, cosine_with_prototype, null, null);
	}

	public void resetHistogram() {

		setLabelsHistogram(new HashMap<String, Double>());
		setSupport(1.0);
	}

	public void resetMeasures() {
		this.pp = 0.0;
		this.pn = 0.0;
		this.np = 0.0;
		this.nn = 0.0;
	}

	public void addPoint(RealHash point, Set<String> contained_labels,
			Double dot_with_prototype, Double rateProts, Double rateHist) {
		proto_norm_SQUARED_updated = false;

		if (this.seen > 0) {

			double weightPrototype = weight_prev(this.seen);
			double weightPoint = weight_new(this.seen);	
			
//			double weightPrototype = (1 - 1/(_currentError + 1));
//			double weightPoint = 1/(_currentError + 1);
			
//			double lambda = 0.01;
//			double gamma = 1/(_currentError + 1);
//			double weightPrototype = (1 - lambda * gamma);
//			double weightPoint = gamma;
			
//			System.out.println("Rates---------------------------------------------------");
//			System.out.println("ProtoRate:" + weightPrototype);
//			System.out.println("PointRate:" + weightPoint);
//			System.out.println("---------------------------------------------------");

			if (!_decayingRate) {
				weightPrototype = 1.0;
				weightPoint = 0.01;
			}

//			if (rateProts != null) {
//				weightPrototype = Math.min(1.0 - rateProts, seen.doubleValue()
//						/ (seen.doubleValue() + 1.0));
//				weightPoint = Math.max(rateProts,
//						1.0 / (seen.doubleValue() + 1.0));
//			}

			prototype.scale(weightPrototype);
			prototype.add(point, weightPoint);
			
			if (_constantNormPrototypes)
				prototype.scale(prototype.norm());
			
			if (proactiveNormComputation && (dot_with_prototype == null)) {
				dot_with_prototype = prototype.dot(point);
			}

			if (dot_with_prototype != null) {
				if (_constantNormPrototypes)
					setProto_norm_SQUARED(1.0);
				else
					setProto_norm_SQUARED((weightPrototype * weightPrototype * getProto_norm_SQUARED())
							+ (2 * weightPrototype * weightPoint * dot_with_prototype)
							+ (weightPoint * weightPoint * 1.0));
				proto_norm_SQUARED_updated = true;
			}

			if ((dot_with_prototype != null) && (radius != null)) {
				double distancePP = (this.getProtoNorm2() + 1.0 - 2 * dot_with_prototype);
				radius = distancePP;
			}

		} else {// initialization

			prototype.add(point, 1.0);
			setProto_norm_SQUARED(1.0);
			proto_norm_SQUARED_updated = true;
			radius = 0.0;
		}

		seen++;
		Double temp = null;

		if (contained_labels != null) {

//			if (rateHist != null) {// FORGET
//				Double weight = 1.0 - rateHist;
//				for (String label : getLabelsHistogram().keySet()) {
//					temp = getLabelsHistogram().get(label);
//					if (temp != null)
//						getLabelsHistogram().put(label, weight * temp);
//				}
//				setSupport(weight * getSupport());
//				sumVals2 = weight * sumVals2;
//			}

			for (String label : contained_labels) {
				temp = getLabelsHistogram().get(label);
				if (temp == null) {
					temp = 0.0;
				} else {
					sumVals2 -= temp * temp;
				}
				getLabelsHistogram().put(label, temp + 1.0);
				sumVals2 += (temp + 1.0) * (temp + 1.0);
			}

			setSupport(getSupport() + 1.0);
		}

		// for(Integer dim: prototype.hash.keySet())
		// variances.getDimScale(dim, prototype);

	}


	public Double getSupport() {
		return support;
	}

	public void setSupport(Double support) {
		this.support = support;
	}

	public Double getProto_norm_SQUARED() {
		return proto_norm_SQUARED;
	}

	public void setProto_norm_SQUARED(Double proto_norm_SQUARED) {
		this.proto_norm_SQUARED = proto_norm_SQUARED;
	}

	public HashMap<String, Double> getLabelsHistogram() {
		return labelsHistogram;
	}

	public void setLabelsHistogram(HashMap<String, Double> labelsHistogram) {
		this.labelsHistogram = labelsHistogram;
	}

}