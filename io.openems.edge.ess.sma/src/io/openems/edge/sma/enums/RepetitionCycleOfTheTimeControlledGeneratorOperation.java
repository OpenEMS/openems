package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum RepetitionCycleOfTheTimeControlledGeneratorOperation implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DAILY(1189, "Daily"), //
	ONCE(2622, "Once"), //
	WEEKLY(2623, "Weekly");

	private final int value;
	private final String name;

	private RepetitionCycleOfTheTimeControlledGeneratorOperation(int value, String name) {
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