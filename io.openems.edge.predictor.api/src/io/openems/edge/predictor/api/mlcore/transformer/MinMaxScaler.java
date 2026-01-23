package io.openems.edge.predictor.api.mlcore.transformer;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class MinMaxScaler<I> extends AbstractSeriesTransformer<I> {

	private static final double EPSILON = 1e-9;

	private final double targetMin;
	private final double targetMax;

	public MinMaxScaler(double min, double max) {
		this.targetMin = min;
		this.targetMax = max;
	}

	public MinMaxScaler() {
		this(0.0, 1.0);
	}

	public MinMaxScaler(double max) {
		this(0.0, max);
	}

	@Override
	protected Series<I> safeTransform(Series<I> series) {
		double dataMin = series.min()//
				.orElseThrow(() -> new IllegalArgumentException("Cannot compute min of empty series"));
		double dataMax = series.max()//
				.orElseThrow(() -> new IllegalArgumentException("Cannot compute max of empty series"));

		double dataRange = dataMax - dataMin;
		double safeDataRange = (Math.abs(dataRange) < EPSILON) ? EPSILON : dataRange;

		double targetRange = this.targetMax - this.targetMin;

		series.apply(v -> this.targetMin + ((v - dataMin) / safeDataRange) * targetRange);
		return series;
	}
}
