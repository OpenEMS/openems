package io.openems.edge.predictor.api.mlcore.transformer;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public abstract class AbstractDualTransformer<I> implements SeriesTransformer<I>, DataFrameTransformer<I> {

	@Override
	public final Series<I> transform(Series<I> series) {
		return this.safeTransform(series.copy());
	}

	@Override
	public DataFrame<I> transform(DataFrame<I> dataframe) {
		return this.safeTransform(dataframe.copy());
	}

	protected abstract Series<I> safeTransform(Series<I> series);

	protected abstract DataFrame<I> safeTransform(DataFrame<I> dataframe);
}