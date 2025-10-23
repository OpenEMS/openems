package io.openems.edge.predictor.api.mlcore.transformer;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public interface DataFrameTransformer<I> extends Transformer<I> {

	/**
	 * Transforms a DataFrame.
	 *
	 * @param dataframe the input DataFrame
	 * @return the transformed DataFrame
	 */
	public DataFrame<I> transform(DataFrame<I> dataframe);
}
