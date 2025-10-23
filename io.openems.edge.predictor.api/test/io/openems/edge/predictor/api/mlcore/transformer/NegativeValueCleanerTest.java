package io.openems.edge.predictor.api.mlcore.transformer;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class NegativeValueCleanerTest {

	@Test
	public void testTransformSeries_ShouldReplaceNegativeValues() {
		var index = List.of(0, 1, 2, 3);
		var values = List.of(1.0, -2.0, 3.0, -4.0);

		var series = new Series<>(index, values);
		var cleaner = new NegativeValueCleaner<Integer>(0.0);

		var result = cleaner.transform(series);

		assertEquals(index, result.getIndex());
		assertEquals(List.of(1.0, 0.0, 3.0, 0.0), result.getValues());
	}

	@Test
	public void testTransformSeries_ShouldIgnoreNullValues() {
		var index = List.of(0, 1, 2);
		var values = Arrays.asList(1.0, null, -1.0);

		var series = new Series<>(index, values);
		var cleaner = new NegativeValueCleaner<Integer>(0.0);

		var result = cleaner.transform(series);

		assertEquals(index, result.getIndex());
		assertEquals(Arrays.asList(1.0, null, 0.0), result.getValues());
	}
}
