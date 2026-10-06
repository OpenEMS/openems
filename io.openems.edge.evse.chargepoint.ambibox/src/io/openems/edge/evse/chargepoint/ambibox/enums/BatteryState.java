package io.openems.edge.evse.chargepoint.ambibox.enums;

import io.openems.common.types.OptionsEnum;

/**
 * State of the EV battery as reported by the charger.
 */
public enum BatteryState implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	SLEEP(0, "Sleep"), //
	IDLE(1, "Idle"), //
	CHARGE(2, "Charge"), //
	DISCHARGE(3, "Discharge");

	private final int value;
	private final String name;

	private BatteryState(int value, String name) {
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
