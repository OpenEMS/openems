package io.openems.edge.fronius.ess.enums;

import io.openems.common.types.OptionsEnum;

public enum OperatingModeForActivePowerLimitation implements OptionsEnum {
	UNDEFINED(-1, "Undefiniert"), 
	NO_CONTROL(0, "Keine Steuerung (Automatik)"),
	SELF_CONSUMPTION(1, "Eigenverbrauch"),
	MANUAL(4, "Manuelle Steuerung (OpenEMS)");

	private final int value;
	private final String name;

	private OperatingModeForActivePowerLimitation(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() { return this.value; }

	@Override
	public String getName() { return this.name; }

	@Override
	public OptionsEnum getUndefined() { return UNDEFINED; }
}