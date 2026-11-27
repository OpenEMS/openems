package io.openems.edge.evse.chargepoint.ambibox.enums;

import io.openems.common.types.OptionsEnum;

/**
 * State of the charger internal inverter.
 */
public enum InverterState implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	OFFLINE(0, "Offline"), //
	SLEEPING(1, "Sleeping"), //
	STARTING(2, "Starting"), //
	MPPT(3, "MPPT"), //
	THROTTLED(4, "Throttled"), //
	SHUTTING_DOWN(5, "Shutting down"), //
	FAULT(6, "Fault"), //
	STANDBY(7, "Standby"), //
	STARTED(8, "Started");

	private final int value;
	private final String name;

	private InverterState(int value, String name) {
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
