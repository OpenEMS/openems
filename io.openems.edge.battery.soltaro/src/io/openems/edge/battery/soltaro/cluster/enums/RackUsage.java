package io.openems.edge.battery.soltaro.cluster.enums;

import io.openems.common.types.OptionsEnum;

public enum RackUsage implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	USED(1, "Rack is used"), //
	UNUSED(2, "Rack is not used");

	private final int value;
	private final String name;

	private RackUsage(int value, String name) {
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