package io.openems.edge.predictor.profileclusteringmodel.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.time.ZonedDateTime;
import java.util.ArrayList;

import org.junit.Test;

import io.openems.edge.predictor.api.common.PredictionException;
import io.openems.edge.predictor.api.common.TrainingException;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.profileclusteringmodel.prediction.PredictionError;
import io.openems.edge.predictor.profileclusteringmodel.training.TrainingError;

public class TimeSeriesPreprocessingServiceTest {

	@Test
	public void testPreprocessTimeSeriesForTraining_ShouldCleanAndInterpolateData() throws Exception {
		var maxGapSizeInterpolation = 3;
		var service = new TimeSeriesPreprocessingService(maxGapSizeInterpolation);

		// Build index for 2 days of 15-minute intervals
		var start = ZonedDateTime.parse("2025-07-10T00:00:00+02:00");
		var index = new ArrayList<ZonedDateTime>();
		for (int i = 0; i < 96 * 2; i++) {
			index.add(start.plusMinutes(15 * i));
		}

		// Build values and include some negative values and NaNs
		var values = new ArrayList<Double>();
		for (int i = 0; i < index.size(); i++) {
			if (i % 20 == 0) {
				values.add(-5.0);
			} else if (i % 37 == 0) {
				values.add(Double.NaN);
			} else {
				values.add((double) i);
			}
		}

		var rawTimeSeries = new Series<>(index, values);

		var preprocessedTimeSeries = service.preprocessTimeSeriesForTraining(rawTimeSeries);

		// Expect two days in DataFrame
		assertEquals(2, preprocessedTimeSeries.rowCount());

		// No NaNs should remain
		preprocessedTimeSeries.getValues().forEach(//
				row -> row.forEach(//
						value -> assertFalse(Double.isNaN(value))));
	}

	@Test
	public void testPreprocessTimeSeriesForTraining_ShouldDropDaysWithTooLargeGaps() {
		var maxGapSizeInterpolation = 2;
		var service = new TimeSeriesPreprocessingService(maxGapSizeInterpolation);

		// Build index for one day of 15-min intervals
		var start = ZonedDateTime.parse("2025-07-10T00:00:00+02:00");
		var index = new ArrayList<ZonedDateTime>();
		for (int i = 0; i < 96; i++) {
			index.add(start.plusMinutes(15 * i));
		}

		// Build values with a large gap exceeding maxGapSizeInterpolation
		var values = new ArrayList<Double>();
		for (int i = 0; i < index.size(); i++) {
			if (i >= 10 && i <= 20) {
				values.add(Double.NaN);
			} else {
				values.add((double) i);
			}
		}

		var rawTimeSeries = new Series<>(index, values);

		// Expect IllegalStateException because after preprocessing, the day is dropped,
		// leaving empty DataFrame
		var exception = assertThrows(TrainingException.class, () -> {
			service.preprocessTimeSeriesForTraining(rawTimeSeries);
		});
		assertEquals(TrainingError.INSUFFICIENT_TRAINING_DATA, exception.getError());
	}

	@Test
	public void testPreprocessTimeSeriesForPrediction_ShouldCleanAndInterpolateData() throws Exception {
		var maxGapSizeInterpolation = 3;
		var service = new TimeSeriesPreprocessingService(maxGapSizeInterpolation);

		// Build index for 2 days of 15-minute intervals
		var start = ZonedDateTime.parse("2025-07-10T00:00:00+02:00");
		var index = new ArrayList<ZonedDateTime>();
		for (int i = 0; i < 96 * 2; i++) {
			index.add(start.plusMinutes(15 * i));
		}

		// Build values and include some negative values and NaNs
		var values = new ArrayList<Double>();
		for (int i = 0; i < index.size(); i++) {
			if (i % 20 == 0) {
				values.add(-5.0);
			} else if (i % 37 == 0) {
				values.add(Double.NaN);
			} else {
				values.add((double) i);
			}
		}

		var rawTimeSeries = new Series<>(index, values);

		var preprocessedTimeSeries = service.preprocessTimeSeriesForPrediction(rawTimeSeries);

		// Expect two days in DataFrame
		assertEquals(2, preprocessedTimeSeries.rowCount());

		// No NaNs should remain
		preprocessedTimeSeries.getValues().forEach(//
				row -> row.forEach(//
						value -> assertFalse(Double.isNaN(value))));
	}

	@Test
	public void testPreprocessTimeSeriesForPrediction_ShouldDropDaysWithTooLargeGaps() {
		var maxGapSizeInterpolation = 2;
		var service = new TimeSeriesPreprocessingService(maxGapSizeInterpolation);

		// Build index for one day of 15-min intervals
		var start = ZonedDateTime.parse("2025-07-10T00:00:00+02:00");
		var index = new ArrayList<ZonedDateTime>();
		for (int i = 0; i < 96; i++) {
			index.add(start.plusMinutes(15 * i));
		}

		// Build values with a large gap exceeding maxGapSizeInterpolation
		var values = new ArrayList<Double>();
		for (int i = 0; i < index.size(); i++) {
			if (i >= 10 && i <= 20) {
				values.add(Double.NaN);
			} else {
				values.add((double) i);
			}
		}

		var rawTimeSeries = new Series<>(index, values);

		// Expect IllegalStateException because after preprocessing, the day is dropped,
		// leaving empty DataFrame
		var exception = assertThrows(PredictionException.class, () -> {
			service.preprocessTimeSeriesForPrediction(rawTimeSeries);
		});
		assertEquals(PredictionError.INSUFFICIENT_PREDICTION_DATA, exception.getError());
	}
}
