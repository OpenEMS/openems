package io.openems.edge.predictor.production.linearmodel.services;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.transformer.DataFrameTransformerPipeline;
import io.openems.edge.predictor.api.mlcore.transformer.TimeOfDaySinCosFeatureTransformer;
import io.openems.edge.predictor.production.linearmodel.ColumnNames;

public class FeatureEngineeringService {

	/**
	 * Transforms the given feature-target matrix for training.
	 *
	 * @param rawFeatureTargetMatrix the {@link DataFrame} containing features and
	 *                               the target column
	 * @return the transformed {@link DataFrame}
	 */
	public DataFrame<ZonedDateTime> transformForTraining(DataFrame<ZonedDateTime> rawFeatureTargetMatrix) {
		return transform(rawFeatureTargetMatrix);
	}

	/**
	 * Transforms the given feature matrix for prediction by applying the same
	 * transformations as in training.
	 *
	 * @param rawFeatureMatrix the {@link DataFrame} containing features
	 * @return the transformed {@link DataFrame}
	 */
	public DataFrame<ZonedDateTime> transformForPrediction(DataFrame<ZonedDateTime> rawFeatureMatrix) {
		return transform(rawFeatureMatrix);
	}

	private static DataFrame<ZonedDateTime> transform(DataFrame<ZonedDateTime> rawFeatureMatrix) {
		var pipeline = new DataFrameTransformerPipeline<>(List.of(//
				new TimeOfDaySinCosFeatureTransformer<>(//
						ColumnNames.TIME_SIN, //
						ColumnNames.TIME_COS, //
						FeatureEngineeringService::utcMinuteOfDay)));

		return pipeline.transform(rawFeatureMatrix);
	}

	private static int utcMinuteOfDay(ZonedDateTime timestamp) {
		// UTC keeps the learned time-of-day relationship stable across DST changes.
		var utcTime = timestamp.withZoneSameInstant(ZoneOffset.UTC);
		return utcTime.getHour() * 60 + utcTime.getMinute();
	}
}