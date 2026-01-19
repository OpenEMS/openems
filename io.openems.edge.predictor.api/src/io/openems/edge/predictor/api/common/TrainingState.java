package io.openems.edge.predictor.api.common;

import io.openems.common.types.OptionsEnum;

public enum TrainingState implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	SUCCESSFUL(0, "Training successful"), //
	DEACTIVATED(1, "Predictor deactivated"), //

	FAILED_UNKNOWN(2, "Unknown error"), //
	FAILED_NO_TRAINING_DATA(4, "No training data"), //
	FAILED_INSUFFICIENT_DATA(5, "Insufficient training data"), //
	;

	private final int value;
	private final String name;

	private TrainingState(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
