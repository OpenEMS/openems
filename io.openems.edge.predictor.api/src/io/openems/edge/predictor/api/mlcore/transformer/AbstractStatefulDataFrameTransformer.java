package io.openems.edge.predictor.api.mlcore.transformer;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public abstract class AbstractStatefulDataFrameTransformer<I> extends AbstractDataFrameTransformer<I>
		implements StatefulDataFrameTransformer<I> {

	private boolean fitted = false;

	@Override
	public final void fit(DataFrame<I> dataframe) {
		this.safeFit(dataframe.copy());
		this.fitted = true;
	}

	@Override
	public final boolean isFitted() {
		return this.fitted;
	}

	protected abstract void safeFit(DataFrame<I> dataframe);
}
