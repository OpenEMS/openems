package io.openems.edge.predictor.api.mlcore.interpolation;

import java.util.List;

public class LinearInterpolator implements Interpolator {

	private final int maxGapSize;

	public LinearInterpolator(int maxGapSize) {
		this.maxGapSize = maxGapSize;
	}

	@Override
	public Double interpolate(int index, List<Double> values) {
		int floorIndex = -1;
		for (int i = index - 1; i >= 0; i--) {
			if (values.get(i) != null && !values.get(i).isNaN()) {
				floorIndex = i;
				break;
			}
		}

		int ceilingIndex = -1;
		for (int i = index + 1; i < values.size(); i++) {
			if (values.get(i) != null && !values.get(i).isNaN()) {
				ceilingIndex = i;
				break;
			}
		}

		if (floorIndex == -1 && ceilingIndex == -1) {
			return Double.NaN;
		}

		if (floorIndex == -1) {
			int gapSize = ceilingIndex;
			if (gapSize > this.maxGapSize) {
				return Double.NaN;
			}
			return values.get(ceilingIndex);
		}

		if (ceilingIndex == -1) {
			int gapSize = values.size() - floorIndex - 1;
			if (gapSize > this.maxGapSize) {
				return Double.NaN;
			}
			return values.get(floorIndex);
		}

		int gapSize = ceilingIndex - floorIndex - 1;
		if (gapSize > this.maxGapSize) {
			return Double.NaN;
		}

		double floorValue = values.get(floorIndex);
		double ceilingValue = values.get(ceilingIndex);

		if (floorValue == ceilingValue) {
			return floorValue;
		}

		double weight = (double) (index - floorIndex) / (ceilingIndex - floorIndex);
		return floorValue + weight * (ceilingValue - floorValue);
	}
}
