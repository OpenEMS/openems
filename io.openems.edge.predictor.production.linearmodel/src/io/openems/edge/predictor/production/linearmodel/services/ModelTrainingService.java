package io.openems.edge.predictor.production.linearmodel.services;

import java.time.ZonedDateTime;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.predictor.api.common.TrainingException;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.regression.Regressor;
import io.openems.edge.predictor.production.linearmodel.ColumnNames;
import io.openems.edge.predictor.production.linearmodel.PredictorConfig.RegressorFitter;
import io.openems.edge.predictor.production.linearmodel.training.TrainingError;

public class ModelTrainingService {

	private final RegressorFitter regressorFitter;
	private final int minTrainingSamples;
	private final int maxTrainingSamples;

	public ModelTrainingService(//
			RegressorFitter regressorFitter, //
			int minTrainingSamples, //
			int maxTrainingSamples) {
		this.regressorFitter = regressorFitter;
		this.minTrainingSamples = minTrainingSamples;
		this.maxTrainingSamples = maxTrainingSamples;
	}

	/**
	 * Trains a {@link Regressor} using the provided feature-target matrix.
	 *
	 * @param featureTargetMatrix the {@link DataFrame} containing features and the
	 *                            target column
	 * @return the trained {@link Regressor}
	 * @throws OpenemsException if the number of rows in the matrix is less than
	 *                          {@link #minTrainingSamplesRequired}
	 */
	public Regressor trainRegressor(DataFrame<ZonedDateTime> featureTargetMatrix) throws TrainingException {
		int sampleCount = featureTargetMatrix.rowCount();

		if (sampleCount < this.minTrainingSamples) {
			throw new TrainingException(TrainingError.INSUFFICIENT_TRAINING_DATA,
					"Insufficient data points for training");
		}

		if (sampleCount > this.maxTrainingSamples) {
			featureTargetMatrix = featureTargetMatrix.tail(this.maxTrainingSamples);
		}

		var features = featureTargetMatrix.copy();
		var target = features.getColumn(ColumnNames.TARGET);
		features.removeColumn(ColumnNames.TARGET);

		return this.regressorFitter.fit(features, target);
	}
}
