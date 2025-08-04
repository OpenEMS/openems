package io.openems.edge.predictor.api.mlcore.transformer;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class MinMaxScalerTest {

	@Test
	public void testTransformSeries_ShouldScaleToZeroOne_WhenNoArgsConstructor() {
		var index = List.of(0, 1, 2);
		var values = List.of(10.0, 20.0, 30.0);
		var series = new Series<>(index, values);

		var scaler = new MinMaxScaler<Integer>();
		var scaled = scaler.transform(series);

		assertEquals(0.0, scaled.get(0), 1e-6);
		assertEquals(0.5, scaled.get(1), 1e-6);
		assertEquals(1.0, scaled.get(2), 1e-6);
	}

	@Test
	public void testTransformSeries_ShouldScaleToCustomMax() {
		var index = List.of(0, 1, 2);
		var values = List.of(0.0, 50.0, 100.0);
		var series = new Series<>(index, values);

		var scaler = new MinMaxScaler<Integer>(2.0);
		var scaled = scaler.transform(series);

		assertEquals(0.0, scaled.get(0), 1e-6);
		assertEquals(1.0, scaled.get(1), 1e-6);
		assertEquals(2.0, scaled.get(2), 1e-6);
	}

	@Test
	public void testTransformSeries_ShouldScaleToCustomRange() {
		var index = List.of(0, 1, 2);
		var values = List.of(5.0, 15.0, 25.0);
		var series = new Series<>(index, values);

		var scaler = new MinMaxScaler<Integer>(50.0, 100.0);
		var scaled = scaler.transform(series);

		assertEquals(50.0, scaled.get(0), 1e-6);
		assertEquals(75.0, scaled.get(1), 1e-6);
		assertEquals(100.0, scaled.get(2), 1e-6);
	}

	@Test
	public void testTransformSeries_ShouldHandleSingleValue() {
		var index = List.of(0);
		var values = List.of(42.0);
		var series = new Series<>(index, values);

		var scaler = new MinMaxScaler<Integer>();
		var scaled = scaler.transform(series);

		assertEquals(0.0, scaled.get(0), 1e-6);
	}
}
