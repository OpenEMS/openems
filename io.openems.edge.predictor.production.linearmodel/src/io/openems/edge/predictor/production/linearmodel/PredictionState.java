package io.openems.edge.predictor.production.linearmodel;

import io.openems.common.types.OptionsEnum;

public enum PredictionState implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	PREDICTING(0, "Predicting successfully"), //
	PREDICTION_FAILED(100, "Prediction failed: Unable to predict future values"), //
	PREDICTION_FAILED_MODEL_LOADING(101, "Prediction failed: Error while loading the model"), //
	PREDICTION_FAILED_MODEL_TOO_OLD(102, "Prediction failed: Model is too old"), //
	PREDICTION_FAILED_WEATHER_FORECAST(103, "Prediction failed: Unable to get weather forecast"), //
	ERROR_UNKNOWN(199, "Unknown error state"), //
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
