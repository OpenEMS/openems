package io.openems.edge.predictor.profileclusteringmodel.services;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.openems.edge.predictor.api.common.PredictionException;
import io.openems.edge.predictor.api.common.TrainingException;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.api.mlcore.interpolation.LinearInterpolator;
import io.openems.edge.predictor.api.mlcore.transformer.DropNaTransformer;
import io.openems.edge.predictor.api.mlcore.transformer.InterpolationTransformer;
import io.openems.edge.predictor.api.mlcore.transformer.NegativeValueCleaner;
import io.openems.edge.predictor.api.mlcore.transformer.SeriesTransformerPipeline;
import io.openems.edge.predictor.profileclusteringmodel.prediction.PredictionError;
import io.openems.edge.predictor.profileclusteringmodel.training.TrainingError;

public class TimeSeriesPreprocessingService {

	private static final int QUARTERS_PER_DAY = 96;

	private final int maxGapSizeInterpolation;

	public TimeSeriesPreprocessingService(int maxGapSizeInterpolation) {
		this.maxGapSizeInterpolation = maxGapSizeInterpolation;
	}

	/**
	 * Preprocesses the given time series for training by cleaning negative values,
	 * interpolating gaps, grouping by day, and dropping days with missing data.
	 *
	 * @param rawTimeSeries the raw time series data
	 * @return a cleaned DataFrame indexed by date
	 * @throws TrainingException if no valid days remain after preprocessing
	 */
	public DataFrame<LocalDate> preprocessTimeSeriesForTraining(Series<ZonedDateTime> rawTimeSeries)
			throws TrainingException {
		var interpolationPipeline = new SeriesTransformerPipeline<ZonedDateTime>(List.of(//
				new NegativeValueCleaner<ZonedDateTime>(Double.NaN), //
				new InterpolationTransformer<ZonedDateTime>(new LinearInterpolator(this.maxGapSizeInterpolation))//
		));
		var interpolatedTimeSeries = interpolationPipeline.transform(rawTimeSeries);

		var timeSeriesByDate = groupTimeSeriesByDate(interpolatedTimeSeries);

		var dropNaTransformer = new DropNaTransformer<LocalDate>();
		var cleanedTimeSeriesByDate = dropNaTransformer.transform(timeSeriesByDate);

		if (cleanedTimeSeriesByDate.rowCount() == 0) {
			throw new TrainingException(TrainingError.INSUFFICIENT_TRAINING_DATA,
					"No valid consumption profiles left after time series preprocessing");
		}
		return cleanedTimeSeriesByDate;
	}

	/**
	 * Preprocesses the given time series for prediction by cleaning negative
	 * values, interpolating gaps, grouping by day, and dropping days with missing
	 * data.
	 *
	 * @param rawTimeSeries the raw time series data
	 * @return a cleaned DataFrame indexed by date
	 * @throws IllegalStateException if no valid days remain after preprocessing
	 */
	public DataFrame<LocalDate> preprocessTimeSeriesForPrediction(Series<ZonedDateTime> rawTimeSeries)
			throws PredictionException {
		var interpolationPipeline = new SeriesTransformerPipeline<ZonedDateTime>(List.of(//
				new NegativeValueCleaner<ZonedDateTime>(Double.NaN), //
				new InterpolationTransformer<ZonedDateTime>(new LinearInterpolator(this.maxGapSizeInterpolation))//
		));
		var interpolatedTimeSeries = interpolationPipeline.transform(rawTimeSeries);

		var timeSeriesByDate = groupTimeSeriesByDate(interpolatedTimeSeries);

		var dropNaTransformer = new DropNaTransformer<LocalDate>();
		var cleanedTimeSeriesByDate = dropNaTransformer.transform(timeSeriesByDate);

		if (cleanedTimeSeriesByDate.rowCount() == 0) {
			throw new PredictionException(PredictionError.INSUFFICIENT_PREDICTION_DATA,
					"No valid consumption profiles left after time series preprocessing");
		}
		return cleanedTimeSeriesByDate;
	}

	private static DataFrame<LocalDate> groupTimeSeriesByDate(Series<ZonedDateTime> series) {
		var grouped = series.toMap().entrySet().stream()//
				.collect(Collectors.groupingBy(//
						entry -> entry.getKey().toLocalDate(), //
						TreeMap::new, //
						Collectors.toMap(//
								Map.Entry::getKey, //
								Map.Entry::getValue)));

		var columnNames = new ArrayList<String>();
		for (int i = 0; i < QUARTERS_PER_DAY; i++) {
			columnNames.add(String.valueOf(i));
		}

		var index = new ArrayList<LocalDate>();
		var data = new ArrayList<List<Double>>();

		for (var entry : grouped.entrySet()) {
			var day = entry.getKey();
			var timeMap = entry.getValue();

			var sortedTimes = new ArrayList<>(timeMap.keySet());
			Collections.sort(sortedTimes);

			var values = sortedTimes.stream().map(timeMap::get).toList();

			if (values.size() != QUARTERS_PER_DAY) {
				throw new IllegalStateException("Expected " + QUARTERS_PER_DAY + " values per day, but got "
						+ values.size() + " for day " + day);
			}

			index.add(day);
			data.add(new ArrayList<>(values));
		}

		return new DataFrame<>(index, columnNames, data);
	}
}
