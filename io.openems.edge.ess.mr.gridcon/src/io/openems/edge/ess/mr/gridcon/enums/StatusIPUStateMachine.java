package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum StatusIPUStateMachine implements OptionsEnum {
	IDLE(2, "Idle"),
	STATUS_3(3, "Unknown"),
	STATUS_6(6, "Unknown"),
	RUN(7, "Run");

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