package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.common.types.OptionsEnum;

public enum StateMachine implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ONGRID_IDLE(11, "On-Grid System is not started"), //
	ONGRID_NORMAL_OPERATION(12, "On-Grid Normal Operation"), //
	ONGRID_ERROR(13, "On-Grid + Error");

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