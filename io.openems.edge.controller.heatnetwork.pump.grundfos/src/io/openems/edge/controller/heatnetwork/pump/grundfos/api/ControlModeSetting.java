package io.openems.edge.controller.heatnetwork.pump.grundfos.api;

import io.openems.common.types.OptionsEnum;

public enum ControlModeSetting implements OptionsEnum  {
	UNDEFINED(-1, "Undefined"), //
	CONST_PRESSURE(0, "Constant pressure"), //
	CONST_FREQUENCY(1, "Constant frequency"), //
	MIN_MOTOR_CURVE(2, "Minimum motor curve"), //
	MAX_MOTOR_CURVE(3, "Maximum motor curve"), //
	AUTO_ADAPT(4, "Auto adapt");

	private int value;
	private String name;

	ControlModeSetting(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
