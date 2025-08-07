package io.openems.edge.predictor.api.mlcore.transformer;

import java.util.List;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public class DataFrameTransformerPipeline<I> {

	private final List<DataFrameTransformer<I>> transformers;

	public DataFrameTransformerPipeline(List<DataFrameTransformer<I>> transformers) {
		this.transformers = transformers;
	}

	/**
	 * Applies all transformers in this pipeline sequentially to the input
	 * {@link DataFrame}.
	 *
	 * @param input the input DataFrame to transform
	 * @return the transformed DataFrame after applying all transformers
	 */
	public DataFrame<I> transform(DataFrame<I> input) {
		var result = input;
		for (DataFrameTransformer<I> transformer : this.transformers) {
			result = result.applyTransformer(transformer);
		}
		return result;
	}
}
