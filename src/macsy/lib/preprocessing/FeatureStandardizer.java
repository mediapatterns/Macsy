package macsy.lib.preprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import macsy.lib.DataPoint;

public class FeatureStandardizer {
	
	public static void standardize(List<DataPoint> data) throws Exception{

		if (data.size() == 0)
			return;

		List<Map<Integer, Double>> allSamples = new ArrayList<Map<Integer, Double>>();
		int N = extractFeaturesFromDataPoints(allSamples, data);

		Map<Integer, Double> featuresMeans = new TreeMap<Integer, Double>();
		for (int j = 1; j < N; j++)
			addVecs(featuresMeans, allSamples.get(j));
		scaleVec(featuresMeans, (double) N);

		Map<Integer, Double> featuresStds = new TreeMap<Integer, Double>();
		for (int j = 0; j < N; j++)
			addVecs(featuresStds, squaredDiff(allSamples.get(j), featuresMeans));
		scaleVec(featuresStds, (double) N);
		powVec(featuresStds, 0.5);

		for (Map<Integer, Double> standardizedP : allSamples) {
			subVecs(standardizedP, featuresMeans);
			scaleVec(standardizedP, featuresStds);
		}
	}

	public static int extractFeaturesFromDataPoints(
			List<Map<Integer, Double>> allSamples, List<DataPoint> data) {
		List<String> ids = new ArrayList<String>();
		for (int j = 0; j < data.size(); j++) {
			allSamples.add(data.get(j).getFeaturesMap());
			ids.add(data.get(j).getID().toString());
		}

		System.out.println(allSamples.size());
		int i = 0;
//		for (Map<Integer, Double> p : allSamples) {
//			System.out.println("FEATUREPROC: " + ids.get(i++) + " " + p.toString());
//		}
		return allSamples.size();
	}
	
	private static void scaleVec(Map<Integer, Double> p, double a) {
		for (Map.Entry<Integer, Double> e : p.entrySet())
			e.setValue(e.getValue() / a);
	}

	private static void scaleVec(Map<Integer, Double> p,
			Map<Integer, Double> pToScale) throws Exception{
		for (Map.Entry<Integer, Double> e : p.entrySet()) {
			Integer i = e.getKey();
			Double featureVal = e.getValue();

//			if (pToScale.get(i) == 0.0)
//				throw new Exception("constant feature (std is 0) !");
//			
			double iStd = pToScale.get(i); // constant features do not count for learning
			p.put(i, iStd > 0.0 ? featureVal / pToScale.get(i) : 0.0);
		}
	}

	private static void addVecs(Map<Integer, Double> p1, Map<Integer, Double> p2) {
		if (p1.size() == 0) {
			p1.putAll(p2);
			return;
		}
		for (Map.Entry<Integer, Double> e : p2.entrySet()) {
			Integer i = e.getKey();
			Double featureVal = e.getValue();
			p1.put(i, p1.get(i) + featureVal);
		}
	}

	private static void subVecs(Map<Integer, Double> p1, Map<Integer, Double> p2) {
		for (Map.Entry<Integer, Double> e : p1.entrySet()) {
			Integer i = e.getKey();
			Double featureVal = e.getValue();

			p1.put(i, featureVal - p2.get(i));
		}
	}

	private static Map<Integer, Double> squaredDiff(Map<Integer, Double> p,
			Map<Integer, Double> pToRemove) {
		Map<Integer, Double> res = new TreeMap<Integer, Double>();

		for (Map.Entry<Integer, Double> e : p.entrySet()) {
			Integer i = e.getKey();
			Double featureVal = e.getValue();
			res.put(i, Math.pow(featureVal - pToRemove.get(i), 2.0));
		}
		return res;
	}

	private static void powVec(Map<Integer, Double> p, double b) {
		for (Map.Entry<Integer, Double> e : p.entrySet()) {
			Integer i = e.getKey();
			Double featureVal = e.getValue();
			p.put(i, Math.pow(featureVal, b));
		}
	}

}
