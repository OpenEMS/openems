package io.openems.edge.predictor.production.linearmodel.prediction;

import io.openems.edge.predictor.api.common.LogSeverity;
import io.openems.edge.predictor.api.common.PredictionState;

public enum PredictionError implements io.openems.edge.predictor.api.common.PredictionError {

	NO_WEATHER_DATA(PredictionState.FAILED_NO_PREDICTION_DATA, LogSeverity.INFO), //
	INVALID_WEATHER_DATA(PredictionState.FAILED_INSUFFICIENT_DATA, LogSeverity.INFO), //
	;

	private final PredictionState failedState;
	private final LogSeverity severity;

	private PredictionError(PredictionState failedState, LogSeverity severity) {
		this.failedState = failedState;
		this.severity = severity;
	}

	@Override
	public PredictionState getFailedState() {
		return this.failedState;
	}

	@Override
	public LogSeverity getSeverity() {
		return this.severity;
	}
}
