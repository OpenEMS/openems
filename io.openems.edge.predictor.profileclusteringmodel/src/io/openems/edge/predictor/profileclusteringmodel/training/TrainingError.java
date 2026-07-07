package io.openems.edge.predictor.profileclusteringmodel.training;

import io.openems.edge.predictor.api.common.LogSeverity;
import io.openems.edge.predictor.api.common.TrainingState;

public enum TrainingError implements io.openems.edge.predictor.api.common.TrainingError {

	UNKNOWN(TrainingState.FAILED_UNKNOWN, LogSeverity.ERROR), //
	NO_CONSUMPTION_DATA(TrainingState.FAILED_NO_TRAINING_DATA, LogSeverity.INFO), //
	INSUFFICIENT_TRAINING_DATA(TrainingState.FAILED_INSUFFICIENT_DATA, LogSeverity.INFO), //
	;

	private final TrainingState failedState;
	private final LogSeverity severity;

	private TrainingError(TrainingState failedState, LogSeverity severity) {
		this.failedState = failedState;
		this.severity = severity;
	}

	@Override
	public TrainingState getFailedState() {
		return this.failedState;
	}

	@Override
	public LogSeverity getSeverity() {
		return this.severity;
	}
}
