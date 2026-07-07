package io.openems.edge.predictor.api.mlcore.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public class LagFeatureTransformerTest {

	@Test
	public void testTransformDataFrame_ShouldAddLaggedFeatureColumn() {
		var dataframe = new DataFrame<>(//
				List.of(0, 1, 2, 3), //
				List.of("column1"), //
				Arrays.asList(//
						Arrays.asList(10.0), //
						Arrays.asList(20.0), //
						Arrays.asList(30.0), //
						Arrays.asList(40.0)));

		var transformer = new LagFeatureTransformer<Integer>("column1", 2, "lag2");
		var result = transformer.transform(dataframe);

		var lagColumn = result.getColumn("lag2");
		assertTrue(Double.isNaN(lagColumn.getValues().get(0)));
		assertTrue(Double.isNaN(lagColumn.getValues().get(1)));
		assertEquals(10.0, lagColumn.getValues().get(2), 1e-6);
		assertEquals(20.0, lagColumn.getValues().get(3), 1e-6);
	}
}
