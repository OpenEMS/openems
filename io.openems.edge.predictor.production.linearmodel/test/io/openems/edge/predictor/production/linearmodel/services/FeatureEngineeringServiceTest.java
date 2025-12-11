package io.openems.edge.predictor.production.linearmodel.services;

import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.production.linearmodel.ColumnNames;

@RunWith(MockitoJUnitRunner.class)
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
}
