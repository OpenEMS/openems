package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.edge.common.channel.OptionsEnum;

public enum StateMachine implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	IDLE(0, "System is not started"), //
	ONGRID_NORMAL_OPERATION(1, "On-Grid Normal Operation");

	private final int value;
	private final String name;

	private StateMachine(int value, String name) {
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