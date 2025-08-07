package io.openems.edge.predictor.api.mlcore.transformer;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public class DayOfWeekFeatureTransformerTest {

	@Test
	public void testTransformDataFrame_ShouldAddDayOfWeekColumn() {
		var index = Arrays.asList(//
				LocalDate.of(2024, 1, 1), // Monday = 1
				LocalDate.of(2024, 1, 2), // Tuesday = 2
				LocalDate.of(2024, 1, 3)); // Wednesday = 3

		var dataframe = new DataFrame<>(//
				index, //
				Arrays.asList("value"), //
				Arrays.asList(//
						Arrays.asList(10.0), //
						Arrays.asList(20.0), //
						Arrays.asList(30.0)));

		var transformer = new DayOfWeekFeatureTransformer<LocalDate>("day_of_week", LocalDate::getDayOfWeek);
		var result = transformer.transform(dataframe);

		var dowColumn = result.getColumn("day_of_week");
		assertEquals(index, dowColumn.getIndex());
		assertEquals(Arrays.asList(1.0, 2.0, 3.0), dowColumn.getValues());
	}
}
