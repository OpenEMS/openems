package io.openems.edge.predictor.api.common;

public interface TrainingError {

	/**
	 * Returns the {@link TrainingState} representing a failed training.
	 *
	 * @return the failed {@link TrainingState}
	 */
	public TrainingState getFailedState();

	/**
	 * Returns the {@link LogSeverity} indicating how severe this training error is
	 * for logging purposes.
	 *
	 * @return the {@link LogSeverity} for this training error
	 */
	public LogSeverity getSeverity();
}
