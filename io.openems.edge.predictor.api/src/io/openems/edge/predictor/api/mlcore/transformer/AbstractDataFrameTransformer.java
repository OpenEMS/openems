package io.openems.edge.predictor.api.mlcore.transformer;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public abstract class AbstractDataFrameTransformer<I> implements DataFrameTransformer<I> {

	@Override
	public final DataFrame<I> transform(DataFrame<I> dataframe) {
		return this.safeTransform(dataframe.copy());
	}

	protected abstract DataFrame<I> safeTransform(DataFrame<I> dataframe);
}