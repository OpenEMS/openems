package io.openems.edge.batteryinverter.sinexcel.enums;

public enum EnableDisable {

	ENABLE("Enable"), //
	DISABLE("Disable");

	private final String value;

	private EnableDisable(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
