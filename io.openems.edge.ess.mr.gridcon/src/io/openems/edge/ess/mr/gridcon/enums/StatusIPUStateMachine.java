package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum StatusIPUStateMachine implements OptionsEnum {
	OFFLINE(0, "Offline"),
	INIT(1, "Init"),
	IDLE(2, "Idle"),
	PRECHARGE(3, "Precharge"),
	GO_IDLE(4, "Go idle"), //TODO are values right?
	READY(6, "Ready"),
	RUN(7, "Run"),
	ERROR(8, "Error"),
	SIA(14, "SIA"),
	FRT(15, "FRT"),
	NOT_DEFINED(16, "Not defined");

	int value;
	String option;

	private StatusIPUStateMachine(int value, String option) {
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