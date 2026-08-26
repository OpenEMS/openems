package io.openems.edge.predictor.api.mlcore.transformer;

import java.util.List;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class SeriesTransformerPipeline<I> {

	private final List<SeriesTransformer<I>> transformers;

	public SeriesTransformerPipeline(List<SeriesTransformer<I>> transformers) {
		this.transformers = transformers;
	}

	/**
	 * Applies all transformers in this pipeline sequentially to the input
	 * {@link Series}.
	 *
	 * @param input the input Series to transform
	 * @return the transformed Series after applying all transformers
	 */
	public Series<I> transform(Series<I> input) {
		var result = input;
		for (SeriesTransformer<I> transformer : this.transformers) {
			result = result.applyTransformer(transformer);
		}
		return result;
	}
}
