package io.openems.edge.fronius.ess.gen24.batteryinverter;

import io.openems.common.types.OptionsEnum;

public enum ControlMode implements OptionsEnum {

	/**
	 * Full control of the Fronius Gen24 storage control registers by OpenEMS.
	 */
	REMOTE(1, "Remote"),
	/**
	 * Disables remote storage control setpoints.
	 */
	INTERNAL(2, "Internal");
	/**
	 * Uses internal mode while the requested setpoint would not change the current
	 * grid balancing, otherwise switches to remote control.
	 */
	//SMART(3, "Smart");

	private final int value;
	private final String name;

	private ControlMode(int value, String name) {
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
		return INTERNAL;
	}

}
