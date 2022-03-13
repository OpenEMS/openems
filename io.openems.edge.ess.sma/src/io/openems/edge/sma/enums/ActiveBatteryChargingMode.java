package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum ActiveBatteryChargingMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	BOOST_CHARGE(1767, "Boost Charge"), //
	FULL(1768, "Full"), //
	EQUALIZATION_CHARGE(1769, "Equalization Charge"), //
	FLOAT_CHARGE(1770, "Float Charge");

	private final int value;
	private final String name;

	private ActiveBatteryChargingMode(int value, String name) {
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