package io.openems.edge.battery.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	UNDEFINED("Undefined", -1), //
	PENDING("Pending", 0), //
	OFF("Off", 1), //
	INIT("Initializing", 2), //
	RUNNING("Running", 3), //
	STOPPING("Stopping", 4), //
	ERROR("Error", 5), //
	ERRORDELAY("Errordelay", 6), //
	STANDBY("Standby", 7);

	private State(String name, int value) {
		this.name = name;
		this.value = value;
	}

	private int value;
	private String name;

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
