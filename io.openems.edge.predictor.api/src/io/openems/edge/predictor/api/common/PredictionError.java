package io.openems.edge.predictor.api.common;

public interface PredictionError {

	/**
	 * Returns the {@link PredictionState} representing a failed prediction.
	 *
	 * @return the failed {@link PredictionState}
	 */
	public PredictionState getFailedState();
}
