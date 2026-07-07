package io.openems.edge.predictor.api.mlcore.transformer;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;

import io.openems.edge.common.meta.types.SubdivisionCode;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public class IsWorkingDayFeatureTransformerTest {

	@Test
	public void testTransformDataFrame_ShouldDetectWorkingAndNonWorkingDays() {
		var index = List.of(//
				LocalDate.of(2024, 1, 2), //
				LocalDate.of(2024, 1, 6), //
				LocalDate.of(2024, 1, 7));
		var values = List.of(//
				List.of(10.0), //
				List.of(20.0), //
				List.of(30.0));

		var dataframe = new DataFrame<>(index, Arrays.asList("value"), values);

		var transformer = new IsWorkingDayFeatureTransformer<LocalDate>(//
				"is_working_day", //
				SubdivisionCode.DE_BY, //
				Function.identity());
		var result = transformer.transform(dataframe);

		var isWorkingDayCol = result.getColumn("is_working_day").getValues();

		assertEquals(1.0, isWorkingDayCol.get(0), 0.0);
		assertEquals(0.0, isWorkingDayCol.get(1), 0.0);
		assertEquals(0.0, isWorkingDayCol.get(2), 0.0);
	}
}
