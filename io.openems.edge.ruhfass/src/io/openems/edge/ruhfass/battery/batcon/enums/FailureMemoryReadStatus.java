package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum FailureMemoryReadStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	READ_NEVER(0, "Failure Memory was never read before!"), // Fehlerspeicher wurde noch nie ausgelesen!
	READ_SUCCESSFUL(1, "Failure Memory read successfully!"), // Fehlerspeicher wurde erfolgreich ausgelesen!
	READ_FAILED(2, "Failure Memory reading failed!"); // Fehlerspiecher lesen war fehlerhaft!

	private int value;
	private String name;

	private FailureMemoryReadStatus(int value, String name) {
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
