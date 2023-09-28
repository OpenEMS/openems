package io.openems.edge.battery.fenecon.f2b.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum OperationState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	FORCE_CHARGE(1, "Force charge"), //
	ONLY_CHARGE(2, "Charge only"), //
	NORMAL(3, "Normal"), //
	ONLY_DISCHARGE(4, "Discharge only")//
	;

	private final int value;
	private final String name;

	private OperationState(int value, String name) {
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
