package io.openems.edge.evse.chargepoint.mennekes.common;

import io.openems.common.types.OptionsEnum;
import io.openems.common.types.SemanticVersion;

public enum DeviceID implements OptionsEnum {
	UNDEFINED(-1, "Undefined", null), //
	PROFESSIONAL(0xEBEE, "Professional Serie", SemanticVersion.fromString("5.22.0")), //
	FOUR_YOU(0x414D, "4You/4Business Serie", SemanticVersion.fromString("1.5.0"));

	private final int value;
	private final String name;
	public final SemanticVersion minVersion;

	private DeviceID(int value, String name, SemanticVersion minVersion) {
		this.value = value;
		this.name = name;
		this.minVersion = minVersion;
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
