package io.openems.edge.predictor.api.mlcore.transformer;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public interface SeriesTransformer<I> extends Transformer<I> {

	/**
	 * Transforms a Series.
	 *
	 * @param series the input Series
	 * @return the transformed Series
	 */
	public Series<I> transform(Series<I> series);
}
