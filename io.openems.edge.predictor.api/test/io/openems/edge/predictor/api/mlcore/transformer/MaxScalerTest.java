package io.openems.edge.predictor.api.mlcore.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;

import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class MaxScalerTest {

	@Test
	public void testConstructor_ShouldThrowException_WhenMaxIsZero() {
		assertThrows(IllegalArgumentException.class, () -> {
			new MaxScaler<>(0);
		});
	}

	@Test
	public void testConstructor_ShouldThrowException_WhenMaxIsNegative() {
		assertThrows(IllegalArgumentException.class, () -> {
			new MaxScaler<>(-5);
		});
	}

	@Test
	public void testTransformSeries_ShouldScaleValuesCorrectly() {
		var index = List.of(0, 1, 2);
		var values = List.of(10.0, 20.0, 30.0);
		var series = new Series<>(index, values);

		var scaler = new MaxScaler<Integer>(10.0);
		var result = scaler.transform(series);

		var expected = List.of(1.0, 2.0, 3.0);
		assertEquals(expected, result.getValues());
	}
}
