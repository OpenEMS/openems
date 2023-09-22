package io.openems.edge.battery.fenecon.f2b.common.enums;

import io.openems.common.types.OptionsEnum;

public enum F2bTerminal15Sw implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	KL_30F_AND_KL_30C_ON(0x6, "0x6: ST_KL (KL 30F and KL 30C On)"), //
	KL_15_ON(0xA, "0xA: ST_KL (KL 15 On)"), //
	;//

	private final int value;
	private final String name;

	private F2bTerminal15Sw(int value, String name) {
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