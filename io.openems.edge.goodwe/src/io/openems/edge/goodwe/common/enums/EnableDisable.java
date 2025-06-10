package io.openems.edge.goodwe.common.enums;

public enum EnableDisable {

	ENABLE("Enable", true), //
	DISABLE("Disable", false);

	private final String value;
	public final boolean booleanValue;

	private EnableDisable(String value, boolean booleanValue) {
		this.value = value;
		this.booleanValue = booleanValue;
	}

	public String getValue() {
		return this.value;
	}
}
