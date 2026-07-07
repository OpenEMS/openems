package io.openems.edge.predictor.api.mlcore.transformer;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.api.mlcore.interpolation.Interpolator;

public class InterpolationTransformerTest {

	@Test
	public void testTransformSeries_ShouldInterpolateMissingValues() {
		var index = Arrays.asList(0, 1, 2);
		var values = Arrays.asList(1.0, null, 3.0);

		var series = new Series<Integer>(index, values);

		Interpolator stubInterpolator = (pos, vals) -> 2.0;

		var transformer = new InterpolationTransformer<Integer>(stubInterpolator);
		var result = transformer.transform(series);

		assertEquals(index, result.getIndex());
		assertEquals(Arrays.asList(1.0, 2.0, 3.0), result.getValues());
	}

	@Test
	public void testTransformSeries_ShouldReturnSameValues() {
		var index = Arrays.asList(0, 1, 2);
		var values = Arrays.asList(1.0, 2.0, 3.0);

		var series = new Series<>(index, values);

		Interpolator stubInterpolator = (pos, vals) -> {
			throw new RuntimeException("Should not be called");
		};

		var transformer = new InterpolationTransformer<Integer>(stubInterpolator);
		var result = transformer.transform(series);

		assertEquals(index, result.getIndex());
		assertEquals(values, result.getValues());
	}
}
