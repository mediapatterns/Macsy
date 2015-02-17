package macsy.lib.preprocessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import macsy.lib.DataPoint;
import macsy.lib.Helpers.BinaryLabel;

public class DataSampleBuilderForRanking implements Iterator<DataPoint> {

	protected static final int BALANCE_TRADEOFF = 100;

	public List<DataPoint> _popularSamples = null;
	public List<DataPoint> _dislikedSamples = null;

	protected int _popularIndex = 0;
	protected int _dislikedIndex = 0;

	protected boolean _balanceData;

	public DataSampleBuilderForRanking() {
		_balanceData = false;
	}

	public DataSampleBuilderForRanking(boolean balanceData) {
		_balanceData = balanceData;
	}

	public Integer nbPopularSamples() {
		if (_popularSamples == null)
			return 0;
		return _popularSamples.size();
	}

	public Integer nbDislikedSamples() {
		if (_dislikedSamples == null)
			return 0;
		return _dislikedSamples.size();
	}

	public boolean collect(DataPoint sample, Set<String> articleLabels) {
		_popularSamples = _popularSamples == null ? new ArrayList<DataPoint>()
				: _popularSamples;
		_dislikedSamples = _dislikedSamples == null ? new ArrayList<DataPoint>()
				: _dislikedSamples;

		/*
		 * Only one label stored for ranking
		 */
		BinaryLabel label = BinaryLabel.valueOf(articleLabels.iterator()
				.next());
		sample.setRealLabel(label.numericValue);
		if (label == BinaryLabel.POSITIVE) {
			if (_balanceData
					&& _popularSamples.size() > _dislikedSamples.size()
							+ BALANCE_TRADEOFF)
				return false;

			_popularSamples.add(sample);
			return true;
		}

		if (_balanceData
				&& _dislikedSamples.size() > _popularSamples.size()
						+ BALANCE_TRADEOFF)
			return false;

		_dislikedSamples.add(sample);
		return true;
	}

	public void reset() {
		_popularIndex = 0;
		_dislikedIndex = 0;
	}

	@Override
	public boolean hasNext() {
		return _popularSamples != null && !_popularSamples.isEmpty()
				&& _dislikedSamples != null && !_dislikedSamples.isEmpty()
				&& _popularIndex < _popularSamples.size();
	}

	private void moveForward() {
		if (_dislikedIndex >= _dislikedSamples.size() - 1) {
			_dislikedIndex = 0;
			++_popularIndex;
			return;
		}
		++_dislikedIndex;
	}

	@Override
	public DataPoint next() {
		if (!hasNext())
			return null;

		DataPoint popularSample = _popularSamples.get(_popularIndex);
		DataPoint dislikedSample = _dislikedSamples.get(_dislikedIndex);
		boolean flippedOutcome = Math.random() >= 0.5;
		DataPoint sample = flippedOutcome ? DataPoint.difference(popularSample,
				dislikedSample) : DataPoint.difference(dislikedSample,
				popularSample);
		sample.setID(popularSample.getID());
		BinaryLabel label = flippedOutcome ? BinaryLabel.POSITIVE
				: BinaryLabel.NEGATIVE;
		sample.setRealLabel(label.numericValue);
		sample.setID(popularSample.getID()); // For storing the date but is not
												// unique among all pairs
		moveForward();

		return sample;
	}

	@Override
	public void remove() {
		if (!hasNext())
			return;

		moveForward();
	}

	public void clear() {
		_popularIndex = 0;
		_dislikedIndex = 0;
		if (_popularSamples != null)
			_popularSamples.clear();
		if (_dislikedSamples != null)
			_dislikedSamples.clear();

	}
}
