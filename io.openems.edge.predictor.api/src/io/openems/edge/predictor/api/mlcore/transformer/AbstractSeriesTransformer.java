package io.openems.edge.predictor.api.mlcore.transformer;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public abstract class AbstractSeriesTransformer<I> implements SeriesTransformer<I> {

	@Override
	public final Series<I> transform(Series<I> series) {
		return this.safeTransform(series.copy());
	}

	protected abstract Series<I> safeTransform(Series<I> series);
}