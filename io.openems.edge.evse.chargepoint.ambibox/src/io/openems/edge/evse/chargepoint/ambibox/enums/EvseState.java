package io.openems.edge.evse.chargepoint.ambibox.enums;

import io.openems.common.types.OptionsEnum;

/**
 * Session State of the EV charger.
 */
public enum EvseState implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	SESSION_SETUP(0, "Session setup"), //
	AUTHORIZATION(1, "Authorization"), //
	CHARGE_PARAMETER_DISCOVERY(2, "Charge parameter discovery"), //
	CABLE_CHECK(3, "Cable check"), //
	PRE_CHARGE(4, "Pre charge"), //
	CHARGE_LOOP(5, "Charge loop"), //
	POST_CHARGE(6, "Post charge"), //
	PAUSED(7, "Paused"), //
	STOPPED(8, "Stopped"), //
	ERROR(9, "Error");

	private final int value;
	private final String name;

	private EvseState(int value, String name) {
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
