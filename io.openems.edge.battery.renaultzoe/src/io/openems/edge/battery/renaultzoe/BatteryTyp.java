package io.openems.edge.battery.renaultzoe;

import io.openems.common.types.OptionsEnum;

public enum BatteryTyp implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NMC(0, "NMC"), //
	LFP(1, "LFP"), //
	LEAD_ACID(2, "Lead-Acic"); //
	
	private int value;
	private String name;

	private BatteryTyp(int value, String name) {
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
