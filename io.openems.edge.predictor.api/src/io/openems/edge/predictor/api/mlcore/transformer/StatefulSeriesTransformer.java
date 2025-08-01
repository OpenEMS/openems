package io.openems.edge.predictor.api.mlcore.transformer;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public interface StatefulSeriesTransformer<I> extends SeriesTransformer<I> {

	/**
	 * Fits the transformer using a Series.
	 *
	 * @param series the data to fit on
	 */
	public void fit(Series<I> series);

	/**
	 * Returns whether the transformer has been fitted.
	 *
	 * @return true if fitted, false otherwise
	 */
	public boolean isFitted();
}
