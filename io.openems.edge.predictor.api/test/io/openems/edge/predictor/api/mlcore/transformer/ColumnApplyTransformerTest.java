package io.openems.edge.predictor.api.mlcore.transformer;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public class ColumnApplyTransformerTest {

	@Test
	public void testTransformDataFrame_ShouldApplyFunctionToSpecifiedColumn() {
		var dataframe = new DataFrame<>(//
				List.of(0, 1), //
				List.of("column1", "column2"), //
				List.of(//
						List.of(-1.0, 2.0), //
						List.of(3.0, -4.0)));

		var transformer = new ColumnApplyTransformer<Integer>(v -> v < 0 ? Double.NaN : v, List.of("column2"));
		var transformed = transformer.transform(dataframe);

		var expected = new DataFrame<>(//
				List.of(0, 1), //
				List.of("column1", "column2"), //
				List.of(//
						List.of(-1.0, 2.0), //
						List.of(3.0, Double.NaN)));

		assertEquals(expected, transformed);
	}
}
