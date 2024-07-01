package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryProtocol implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	GOODWE(0x122, "SECU-S / LX S-H / LX F-H"), //
	PYLONTECH(0x101, "Powercube H1 / Force H1 / Force H2"), //
	BYD_BOX_H(0x102, "Byd box h"), //
	BYD_BOX_PREMIUM_HVS(0x106, "Byd box premium hvs"), //
	BYD_BOX_PREMIUM_HVM_OR_HVL(0x105, "Byd box premium hvm / hvl"), //
	LG(0x104, "Resu_hv_type-r"), //
	OLOID_DYNESS_SOLUNA(0x11E, "For Oloid is LBS, For DYNESS is Tower, For SOLUNA is HV Battery"), //
	EMS_USE(0x11F, "EMS Battery"); //

	private final int value;
	private final String option;

	private BatteryProtocol(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}