package io.openems.edge.battery.soltaro.cluster.enums;

import io.openems.common.types.OptionsEnum;

public enum ClusterStartStop implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	START(1, "Start"), //
	STOP(2, "Stop");

	int value;
	String name;

	private ClusterStartStop(int value, String name) {
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