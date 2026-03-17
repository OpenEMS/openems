package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

// ToDo
public enum RemoteDispatchSystemLimitSwitch implements OptionsEnum {
	UNDEFINED(-1, "Undefined"),
	DISABLE(0, "No limitation"),
	IMPORT_LIMIT_ENABLE(1, "Import limit enabled"),
	EXPORT_LIMIT_ENABLE(2, "Export limit enabled"),
	IMPORT_EXPORT_LIMIT_ENABLE(3, "Import and export limit enabled");
	private final int value;
	private final String name;

	RemoteDispatchSystemLimitSwitch(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override public int getValue() { return this.value; }
	@Override public String getName() { return this.name; }
	@Override public OptionsEnum getUndefined() { return UNDEFINED; }
}
