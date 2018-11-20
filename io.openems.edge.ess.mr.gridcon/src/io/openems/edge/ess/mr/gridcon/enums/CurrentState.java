package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.edge.common.channel.doc.OptionsEnum;

// TODO numbers are not correctly
public enum CurrentState implements OptionsEnum { // see Software manual chapter 5.1
	UNDEFINED(-1, "Undefined"), //
	OFFLINE(1, "Offline"), //
	INIT(2, "Init"), //
	IDLE(3, "Idle"), //
	PRECHARGE(4, "Precharge"), //
	STOP_PRECHARGE(5, "Stop precharge"), //
	ECO(6, "Eco"), //
	PAUSE(7, "Pause"), //
	RUN(8, "Run"), //
	ERROR(99, "Error");

	private final int value;
	private final String name;

	private CurrentState(int value, String name) {
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