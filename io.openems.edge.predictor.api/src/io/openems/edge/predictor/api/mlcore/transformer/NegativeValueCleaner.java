package io.openems.edge.predictor.api.mlcore.transformer;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class NegativeValueCleaner<I> extends AbstractSeriesTransformer<I> {

	private final Double replacementValue;

	public NegativeValueCleaner(Double replacementValue) {
		this.replacementValue = replacementValue;
	}

	@Override
	protected Series<I> safeTransform(Series<I> series) {
		var cleanedValues = series.getValues().stream()//
				.map(v -> (v != null && v.doubleValue() < 0) //
						? this.replacementValue //
						: v)//
				.toList();

		return new Series<>(series.getIndex(), cleanedValues);
	}
}
