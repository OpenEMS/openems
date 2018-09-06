package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum PControlMode implements OptionsEnum {
	DISABLED(1, "Disabled"), // TODO Check values!!!
	ACTIVE_POWER_CONTROL(0x3F80, "Active Power Control Mode"),
	POWER_LIMITER(4, "Power Limiter Mode");

	private int value;
	String option;

	private PControlMode(int value, String option) {
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
