package io.openems.edge.predictor.api.mlcore.transformer;

import java.util.ArrayList;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.api.mlcore.interpolation.Interpolator;

public class InterpolationTransformer<I> extends AbstractSeriesTransformer<I> {

	private final Interpolator interpolator;

	public InterpolationTransformer(Interpolator interpolator) {
		this.interpolator = interpolator;
	}

	@Override
	protected Series<I> safeTransform(Series<I> series) {
		var values = series.getValues();
		var interpolatedValues = new ArrayList<Double>();

		for (int i = 0; i < values.size(); i++) {
			var value = values.get(i);
			if (value == null || Double.isNaN(value)) {
				value = this.interpolator.interpolate(i, values);
			}
			interpolatedValues.add(value);
		}

		return new Series<>(series.getIndex(), interpolatedValues);
	}
}