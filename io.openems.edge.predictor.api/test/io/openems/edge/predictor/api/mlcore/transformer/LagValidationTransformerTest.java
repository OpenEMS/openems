package io.openems.edge.predictor.api.mlcore.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public class LagValidationTransformerTest {

	@Test
	public void testTransformDataFrame_ShouldInvalidateWrongLag() {
		var index = Arrays.asList(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 4));
		var values = Arrays.asList(Arrays.asList(10.0), Arrays.asList(20.0), Arrays.asList(30.0));

		var dataframe = new DataFrame<>(index, Arrays.asList("value"), values);

		var transformer = new LagValidationTransformer<LocalDate>("value", 1, ChronoUnit.DAYS);
		var result = transformer.transform(dataframe);

		assertEquals(10.0, result.getValueAt(0, 0), 1e-6);
		assertEquals(20.0, result.getValueAt(1, 0), 1e-6);
		assertTrue(Double.isNaN(result.getValueAt(2, 0)));
	}
}
