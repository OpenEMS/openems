package io.openems.edge.predictor.api.common;

import io.openems.common.types.OptionsEnum;

public enum PredictionState implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	SUCCESSFUL(0, "Prediction successful"), //
	DEACTIVATED(1, "Predictor deactivated"), //

	FAILED_UNKNOWN(2, "Unknown error"), //
	FAILED_NO_MODEL(3, "No model trained"), //
	FAILED_MODEL_OUTDATED(4, "Model outdated"), //
	FAILED_NO_PREDICTION_DATA(5, "No prediction data"), //
	FAILED_INSUFFICIENT_DATA(6, "Insufficient prediction data"), //
	;

	private final int value;
	private final String name;

	private PredictionState(int value, String name) {
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
