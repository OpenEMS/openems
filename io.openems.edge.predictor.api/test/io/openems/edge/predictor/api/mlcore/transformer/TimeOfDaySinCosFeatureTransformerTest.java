package io.openems.edge.predictor.api.mlcore.transformer;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public class TimeOfDaySinCosFeatureTransformerTest {

	@Test
	public void testTransformDataFrame_ShouldAddSinAndCosColumns() {
		var index = Arrays.asList(0, 360, 720, 1080);

		var dataframe = new DataFrame<>(//
				index, //
				Arrays.asList("column1"), //
				Arrays.asList(//
						Arrays.asList(10.0), //
						Arrays.asList(20.0), //
						Arrays.asList(30.0), //
						Arrays.asList(40.0)));

		var transformer = new TimeOfDaySinCosFeatureTransformer<Integer>(//
				"time_sin", //
				"time_cos", //
				i -> i);

		var result = transformer.transform(dataframe);

		final var expectedSin = Arrays.asList(//
				Math.sin(2 * Math.PI * 0.0 / 1440), //
				Math.sin(2 * Math.PI * 360.0 / 1440), //
				Math.sin(2 * Math.PI * 720.0 / 1440), //
				Math.sin(2 * Math.PI * 1080.0 / 1440));

		final var expectedCos = Arrays.asList(//
				Math.cos(2 * Math.PI * 0.0 / 1440), //
				Math.cos(2 * Math.PI * 360.0 / 1440), //
				Math.cos(2 * Math.PI * 720.0 / 1440), //
				Math.cos(2 * Math.PI * 1080.0 / 1440));

		var sinColumn = result.getColumn("time_sin");
		assertEquals(index, sinColumn.getIndex());
		assertEquals(expectedSin, sinColumn.getValues());

		var cosColumn = result.getColumn("time_cos");
		assertEquals(index, cosColumn.getIndex());
		assertEquals(expectedCos, cosColumn.getValues());
	}
}
