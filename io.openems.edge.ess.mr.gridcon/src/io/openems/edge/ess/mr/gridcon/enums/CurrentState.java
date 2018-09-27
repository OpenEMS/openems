package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.edge.common.channel.doc.OptionsEnum;

// TODO numbers are not correctly
public enum CurrentState implements OptionsEnum { // see Software manual chapter 5.1
	OFFLINE(1, "Offline"),
	INIT(2, "Init"),
	IDLE(3, "Idle"),
	PRECHARGE(4, "Precharge"),
	STOP_PRECHARGE(5, "Stop precharge"),
	ECO(6, "Eco"),
	PAUSE(7, "Pause"),
	RUN(8, "Run"),
	ERROR(99, "Error");

	int value;
	String option;

	private CurrentState(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getOption() {
		return option;
	}
}