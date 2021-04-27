package io.openems.edge.heater.heatpump.tecalor.api;

import io.openems.common.types.OptionsEnum;

public enum OperatingMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NOTBETRIEB(0, "Notbetrieb"), //
	BEREITSCHAFTSBETRIEB(1, "Bereitschaftsbetrieb"), //
	PROGRAMMBETRIEB(2, "Programmbetrieb"), //
	KOMFORTBETRIEB(3, "Komfortbetrieb"), //
	ECOBETRIEB(4, "ECO-Betrieb"), //
	WARMWASSERBETRIEB(5, "Warmwasserbetrieb"); //

	private int value;
	private String name;

	private OperatingMode(int value, String name) {
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