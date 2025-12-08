package io.openems.edge.batteryinverter.victron.ess.symmetric;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMAL(0, "Normal"), // SoC in range between min and max
	ERROR(1, "Error State"), //
	IDLE(3, "Idle state e.g. power from grid too low"),;

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
