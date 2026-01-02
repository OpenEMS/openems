package io.openems.edge.predictor.profileclusteringmodel.prediction;

import io.openems.edge.predictor.api.common.PredictionState;

public enum PredictionError implements io.openems.edge.predictor.api.common.PredictionError {

	NO_CONSUMPTION_DATA(PredictionState.FAILED_NO_PREDICTION_DATA), //
	INSUFFICIENT_PREDICTION_DATA(PredictionState.FAILED_INSUFFICIENT_DATA), //
	;

	private final PredictionState failedState;

	private PredictionError(PredictionState failedState) {
		this.failedState = failedState;
	}

	public PredictionState getFailedState() {
		return this.failedState;
	}
}
