package io.openems.edge.ess.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum OperatingModeForActivePowerLimitation implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(303, "Off"), //
	ACT_PWR_LIM_P_IN_W(1077, "Active Power Limitation P in W"), //
	ACT_PWR_LIM_AS_PERCENT_OF_PMAX(1078, "Act. Power Lim. as % of Pmax"), //
	ACT_PWR_LIM_VIA_PV_SYSTEM_CONTROL(1079, "Act. Power Lim. via PV System Control"), //
	ACT_PWR_LIM_P_VIA_ANALOG_INPUT(1390, "Act. Power Lim. P via Analog Input"), //
	ACT_PWR_LIM_P_VIA_DIGITAL_INPUT(1391, "Act. Power Lim. P via Digital Input");

	private final int value;
	private final String name;

	private OperatingModeForActivePowerLimitation(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}