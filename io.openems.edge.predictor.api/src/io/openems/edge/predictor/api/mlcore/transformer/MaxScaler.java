package io.openems.edge.predictor.api.mlcore.transformer;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class MaxScaler<I> extends AbstractSeriesTransformer<I> {

	private final double max;

	public MaxScaler(double max) {
		if (max <= 0) {
			throw new IllegalArgumentException("Global max must be positive");
		}
		this.max = max;
	}

	@Override
	protected Series<I> safeTransform(Series<I> series) {
		series.apply(v -> v / this.max);
		return series;
	}
}
