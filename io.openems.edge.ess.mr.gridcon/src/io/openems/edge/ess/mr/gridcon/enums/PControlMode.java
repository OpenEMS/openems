package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum PControlMode implements OptionsEnum {
	DISABLED(1, "Disabled"), // TODO Check values!!!
	ACTIVE_POWER_CONTROL(1, "Active Power Control Mode"), //TODO maybe inverted word order?!
	POWER_LIMITER(4, "Power Limiter Mode");

	private float value;
	String option;

	private PControlMode(float value, String option) {
		this.value = value;
		this.option = option;
	}

	
	public float getFloatValue() {
		return value;
	}

	@Override
	public String getOption() {
		return option;
	}


	@Override
	public int getValue() {
		return 0;
	}
}
