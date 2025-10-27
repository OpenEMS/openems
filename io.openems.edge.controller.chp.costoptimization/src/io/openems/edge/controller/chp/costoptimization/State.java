package io.openems.edge.controller.chp.costoptimization;

import io.openems.common.types.OptionsEnum;

// ToDo
public enum State implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMAL(0, "Normal"), // SoC in range between min and max
	ERROR(1, "Error State"),
	CHP_ACTIVE(2, "CHP activated"),
	CHP_INACTIVE(3, "CHP stopped"),
	CHP_PREPARING(4, "CHP preparing"),
	IDLE(5, "Idle state e.g. power from grid too low"),
	OVER_TEMPERATURE(6, "Idle state e.g. power from grid too low"),
	CHP_NOT_READY(7, "Hardware is not ready to start"),

	;


	private final int value;
	private final String name;

	private State(int value, String name) {
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