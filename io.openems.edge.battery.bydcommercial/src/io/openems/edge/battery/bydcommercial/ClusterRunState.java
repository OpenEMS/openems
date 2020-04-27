package io.openems.edge.battery.bydcommercial;

import io.openems.common.types.OptionsEnum;

public enum ClusterRunState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMAL(0, "Normal"), //
	FULL(0x1, "Full"), //
	EMPTY(0x2, "Empty"), //
	STANDBY(0x3, "Standby"), //
	STOP(0x4, "Stop");

	private int value;
	private String name;

	private ClusterRunState(int value, String name) {
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