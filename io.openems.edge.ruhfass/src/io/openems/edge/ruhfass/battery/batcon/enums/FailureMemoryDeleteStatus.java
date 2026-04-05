package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum FailureMemoryDeleteStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DELETE_NEVER(0, "Failure Memory was never deleted before!"), // Fehlerspeicher wurde noch nie gel�scht!
	DELETE_SUCCESSFUL(1, "Failure Memory deleted successfully!"), // Fehlerspeicher wurde erfolgreich gel�scht!
	DELETE_FAILED(2, "Failure Memory deleting failed!"), // Fehlerspiecher l�schen war fehlerhaft!
	DELETE_TRIGGER(3, "Failure Memory delete is triggered!"), // Fehlerspiecher l�schen wird getriggert!
	NO_ANSWER(4, "No Answer"); // Keine Antwort

	private int value;
	private String name;

	private FailureMemoryDeleteStatus(int value, String name) {
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
