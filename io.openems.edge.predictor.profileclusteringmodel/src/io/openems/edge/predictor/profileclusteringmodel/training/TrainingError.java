package io.openems.edge.predictor.profileclusteringmodel.training;

import io.openems.edge.predictor.api.common.TrainingState;

public enum TrainingError implements io.openems.edge.predictor.api.common.TrainingError {

	UNKNOWN(TrainingState.FAILED_UNKNOWN), //
	NO_CONSUMPTION_DATA(TrainingState.FAILED_NO_TRAINING_DATA), //
	INSUFFICIENT_TRAINING_DATA(TrainingState.FAILED_INSUFFICIENT_DATA), //
	;

	private final TrainingState failedState;

	private TrainingError(TrainingState failedState) {
		this.failedState = failedState;
	}

	@Override
	public TrainingState getFailedState() {
		return this.failedState;
	}
}
