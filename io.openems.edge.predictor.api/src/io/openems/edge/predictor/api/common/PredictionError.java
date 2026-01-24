package io.openems.edge.predictor.api.common;

public interface PredictionError {

	/**
	 * Returns the {@link PredictionState} representing a failed prediction.
	 *
	 * @return the failed {@link PredictionState}
	 */
	public PredictionState getFailedState();

	/**
	 * Returns the {@link LogSeverity} indicating how severe this prediction error
	 * is for logging purposes.
	 *
	 * @return the {@link LogSeverity} for this prediction error
	 */
	public LogSeverity getSeverity();
}
