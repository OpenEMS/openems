package io.openems.edge.predictor.api.mlcore.transformer;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public abstract class AbstractStatefulDualTransformer<I> extends AbstractDualTransformer<I>
		implements StatefulSeriesTransformer<I>, StatefulDataFrameTransformer<I> {

	private boolean fitted = false;

	@Override
	public final void fit(Series<I> series) {
		this.safeFit(series.copy());
		this.fitted = true;
	}

	@Override
	public final void fit(DataFrame<I> dataframe) {
		this.safeFit(dataframe.copy());
		this.fitted = true;
	}

	@Override
	public final boolean isFitted() {
		return this.fitted;
	}

	protected abstract void safeFit(Series<I> series);

	protected abstract void safeFit(DataFrame<I> dataframe);
}
