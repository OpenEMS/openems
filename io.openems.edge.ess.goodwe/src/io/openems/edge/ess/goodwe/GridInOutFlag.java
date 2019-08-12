package io.openems.edge.ess.goodwe;

import io.openems.common.types.OptionsEnum;

enum GridInOutFlag implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NEITHER_SEND_NOR_GET(0, "Inverter neither send power to Grid, nor get power from Grid."), //
	SEND(1, "Inverter sends power to Grid"), //
	GET(2, "Inverter gets power from Grid."); //
	
	
	
	private int value;
	private String option;

	private GridInOutFlag(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return option;
	}
	
	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}	
}