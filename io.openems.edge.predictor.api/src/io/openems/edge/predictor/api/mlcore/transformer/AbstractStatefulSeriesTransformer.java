package io.openems.edge.predictor.api.mlcore.transformer;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public abstract class AbstractStatefulSeriesTransformer<I> extends AbstractSeriesTransformer<I>
		implements StatefulSeriesTransformer<I> {

	private boolean fitted = false;

	@Override
	public final void fit(Series<I> series) {
		this.safeFit(series.copy());
		this.fitted = true;
	}

	@Override
	public final boolean isFitted() {
		return this.fitted;
	}

	protected abstract void safeFit(Series<I> series);
}
