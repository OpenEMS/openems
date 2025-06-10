package io.openems.edge.evcs.webasto.next.enums;

import io.openems.common.types.OptionsEnum;

public enum StartCancelChargingSession implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_ACTION(0, "No action"), //
	START_CHARGING_SESSION(1, "Start charging session"), //
	CANCEL_CHARGING_SESSION(2, "Cancel charging session ") //
	;

	private final int value;
	private final String name;

	private StartCancelChargingSession(int value, String name) {
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
