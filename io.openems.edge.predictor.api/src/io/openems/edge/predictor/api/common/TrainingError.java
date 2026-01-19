package io.openems.edge.predictor.api.common;

public interface TrainingError {

	/**
	 * Returns the {@link TrainingState} representing a failed training.
	 *
	 * @return the failed {@link TrainingState}
	 */
	public TrainingState getFailedState();
}
