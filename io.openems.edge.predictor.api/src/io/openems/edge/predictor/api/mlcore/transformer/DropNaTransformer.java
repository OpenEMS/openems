package io.openems.edge.predictor.api.mlcore.transformer;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class DropNaTransformer<I> extends AbstractDualTransformer<I> {

	@Override
	protected Series<I> safeTransform(Series<I> series) {
		series.dropNa();
		return series;
	}

	@Override
	protected DataFrame<I> safeTransform(DataFrame<I> dataframe) {
		dataframe.dropNa();
		return dataframe;
	}
}
