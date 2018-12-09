package io.openems.edge.controller.ess.limittotaldischarge;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum State implements OptionsEnum {
	
	NORMAL(0, "Normal"), //
	MIN_SOC(1, "Min-SoC"), //
	FORCE_CHARGE_SOC(2, "Force-Charge-SoC");

	private final int value;
	private final String option;

	private State(int value, String option) {
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