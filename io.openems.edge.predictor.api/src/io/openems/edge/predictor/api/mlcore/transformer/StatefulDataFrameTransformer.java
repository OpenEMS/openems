package io.openems.edge.predictor.api.mlcore.transformer;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public interface StatefulDataFrameTransformer<I> extends DataFrameTransformer<I> {

	/**
	 * Fits the transformer using a DataFrame.
	 *
	 * @param dataframe the data to fit on
	 */
	public void fit(DataFrame<I> dataframe);

	/**
	 * Returns whether the transformer has been fitted.
	 *
	 * @return true if fitted, false otherwise
	 */
	public boolean isFitted();
}
