package io.openems.edge.predictor.production.linearmodel.services;

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
		var pipeline = new DataFrameTransformerPipeline<ZonedDateTime>(List.of(//
				new TimeOfDaySinCosFeatureTransformer<>(//
						ColumnNames.TIME_SIN, //
						ColumnNames.TIME_COS, //
						dt -> dt.getHour() * 60 + dt.getMinute())));

		var transformedFeatureTargetMatrix = pipeline.transform(rawFeatureTargetMatrix);

		return transformedFeatureTargetMatrix;
	}

	/**
	 * Transforms the given feature matrix for prediction by applying the same
	 * transformations as in training.
	 *
	 * @param rawFeatureMatrix the {@link DataFrame} containing features
	 * @return the transformed {@link DataFrame}
	 */
	public DataFrame<ZonedDateTime> transformForPrediction(DataFrame<ZonedDateTime> rawFeatureMatrix) {
		var pipeline = new DataFrameTransformerPipeline<ZonedDateTime>(List.of(//
				new TimeOfDaySinCosFeatureTransformer<>(//
						ColumnNames.TIME_SIN, //
						ColumnNames.TIME_COS, //
						dt -> dt.getHour() * 60 + dt.getMinute())));

		var transformedFeatureMatrix = pipeline.transform(rawFeatureMatrix);

		return transformedFeatureMatrix;
	}
}