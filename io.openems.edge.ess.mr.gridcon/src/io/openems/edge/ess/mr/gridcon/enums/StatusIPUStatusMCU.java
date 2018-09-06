package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum StatusIPUStatusMCU implements OptionsEnum {
	IDLE(4, "Idle"),
	STATUS_8(8, "Unknown"),
	STATUS_12(12, "Unknown"),
	RUN(14, "Run");

	int value;
	String option;

	private StatusIPUStatusMCU(int value, String option) {
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