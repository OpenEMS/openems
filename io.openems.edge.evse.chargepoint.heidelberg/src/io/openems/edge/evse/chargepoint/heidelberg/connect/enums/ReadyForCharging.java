package io.openems.edge.evse.chargepoint.heidelberg.connect.enums;

import io.openems.common.types.OptionsEnum;

// Could be removed when the register can be set directly at the modbus mapping and means the same as the Nature-Channel
public enum ReadyForCharging implements OptionsEnum {

	UNDEFINED(-1, "Undefined", false), //
	AVAILABLE(0, "Wallbox available", false), //
	READY(1, "Wallbox ready for charging", false);

	public final int value;
	public final String name;
	public final boolean isReady;

	private ReadyForCharging(int value, String name, boolean isReady) {
		this.value = value;
		this.name = name;
		this.isReady = isReady;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name();
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
