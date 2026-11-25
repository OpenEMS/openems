package io.openems.edge.predictor.production.linearmodel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.production.linearmodel.ColumnNames;

public class FeatureEngineeringServiceTest {

	private ZonedDateTime now = ZonedDateTime.of(2025, 9, 10, 15, 30, 0, 0, ZoneId.of("Europe/Berlin"));

	@Test
	public void testTransformForTraining_ShouldReturnExpectedMatrix() {
		var index = List.of(//
				this.now.minusMinutes(30), //
				this.now.minusMinutes(15));
		var columnNames = List.of("column1", ColumnNames.TARGET);
		var values = List.of(//
				List.of(1.0, 100.0), //
				List.of(2.0, 200.0));
		var input = new DataFrame<>(index, columnNames, values);

		var sut = new FeatureEngineeringService();
		var transformed = sut.transformForTraining(input);

		assertEquals(index, transformed.getIndex());
		assertEquals(//
				List.of("column1", ColumnNames.TARGET, ColumnNames.TIME_SIN, ColumnNames.TIME_COS), //
				transformed.getColumnNames());
		assertEquals(List.of(1.0, 2.0), transformed.getColumn("column1").getValues());
		assertEquals(List.of(100.0, 200.0), transformed.getColumn(ColumnNames.TARGET).getValues());
	}

	@Test
	public void testTransformForPrediction_ShouldReturnExpectedMatrix() {
		var index = List.of(//
				this.now.plusMinutes(15), //
				this.now.plusMinutes(30));
		var columnNames = List.of("column1");
		var values = List.of(//
				List.of(1.0), //
				List.of(2.0));
		var input = new DataFrame<>(index, columnNames, values);

		var sut = new FeatureEngineeringService();
		var transformed = sut.transformForPrediction(input);

		assertEquals(index, transformed.getIndex());
		assertEquals(//
				List.of("column1", ColumnNames.TIME_SIN, ColumnNames.TIME_COS), //
				transformed.getColumnNames());
		assertEquals(List.of(1.0, 2.0), transformed.getColumn("column1").getValues());
	}

	@ParameterizedTest
	@CsvSource({ //
			"2026-03-28T13:15+01:00[Europe/Berlin], 735", //
			"2026-03-30T14:15+02:00[Europe/Berlin], 735", //
			"2026-03-29T01:45+01:00[Europe/Berlin], 45", //
			"2026-03-29T03:00+02:00[Europe/Berlin], 60", //
			"2026-10-24T14:15+02:00[Europe/Berlin], 735", //
			"2026-10-26T13:15+01:00[Europe/Berlin], 735", //
			"2026-10-25T02:45+02:00[Europe/Berlin], 45", //
			"2026-10-25T02:00+01:00[Europe/Berlin], 60", //
			"2026-10-25T02:30+02:00[Europe/Berlin], 30", //
			"2026-10-25T02:30+01:00[Europe/Berlin], 90" //
	})
	void testTimeFeaturesUseUtcForTrainingAndPrediction(String timestamp, int expectedUtcMinuteOfDay) {
		var index = List.of(ZonedDateTime.parse(timestamp));
		var trainingInput = new DataFrame<>(index, List.of("column1", ColumnNames.TARGET),
				List.of(List.of(1.0, 100.0)));
		var predictionInput = new DataFrame<>(index, List.of("column1"), List.of(List.of(1.0)));
		var sut = new FeatureEngineeringService();

		var training = sut.transformForTraining(trainingInput);
		var prediction = sut.transformForPrediction(predictionInput);

		double expectedAngle = 2 * Math.PI * expectedUtcMinuteOfDay / 1_440;
		for (var transformed : List.of(training, prediction)) {
			assertEquals(index, transformed.getIndex());
			assertEquals(Math.sin(expectedAngle), transformed.getColumn(ColumnNames.TIME_SIN).getValues().getFirst(),
					1e-12);
			assertEquals(Math.cos(expectedAngle), transformed.getColumn(ColumnNames.TIME_COS).getValues().getFirst(),
					1e-12);
		}
		assertEquals(training.getColumn(ColumnNames.TIME_SIN).getValues(),
				prediction.getColumn(ColumnNames.TIME_SIN).getValues());
		assertEquals(training.getColumn(ColumnNames.TIME_COS).getValues(),
				prediction.getColumn(ColumnNames.TIME_COS).getValues());
	}
}
