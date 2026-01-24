package io.openems.edge.predictor.production.linearmodel;

import java.time.Instant;

import io.openems.edge.predictor.api.common.TrainingError;
import io.openems.edge.predictor.api.mlcore.regression.Regressor;

public interface TrainingCallback {

	/**
	 * Called when the training starts.
	 */
	public void onTrainingStart();

	/**
	 * Called when training completes successfully.
	 *
	 * @param bundle the trained model
	 */
	public void onTrainingSuccess(ModelBundle bundle);

	/**
	 * Called when training fails.
	 *
	 * @param error   the training error type
	 * @param message additional detail or cause
	 */
	public void onTrainingError(TrainingError error, String message);

	public record ModelBundle(//
			Regressor regressor, //
			Instant createdAt//
	) {
	}
}
