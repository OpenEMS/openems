package io.openems.edge.predictor.production.linearmodel;

import io.openems.common.types.OptionsEnum;

public enum TrainingState implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	TRAINING(0, "Training in progress"), //
	TRAINED(1, "Model trained"), //
	TRAINING_FAILED_WEATHER_FETCH(100, "Training failed: Unable to fetch weather data"), //
	TRAINING_FAILED_PRODUCTION_FETCH(101, "Training failed: Unable to fetch production data"), //
	TRAINING_FAILED_INSUFFICIENT_DATA(102, "Training failed: Insufficient training data"), //
	TRAINING_FAILED_DATA_VALIDATION(103, "Training failed: Data validation or cleaning error"), //
	TRAINING_FAILED_MODEL_SAVING(104, "Training failed: Error while saving the model"), //
	TRAINING_FAILED_MODEL_TRAINING(105, "Training failed: Error during model training"), //
	ERROR_UNKNOWN(199, "Unknown error state"), //
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
