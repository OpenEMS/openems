package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryType implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	VALVE_REGULATED_LEAD_ACID_BATTERY(1782, "Valve-Regulated Lead-Acid Battery (VRLA)"), //
	FLOODED_LEAD_ACID_BATTERY(1783, "Flooded Lead-Acid Battery (FLA)"), //
	NICKEL_CADMIUM(1784, "Nickel/Cadmium (NiCd)"), //
	LITHIUM_ION(1785, "Lithium-Ion (Li-Ion)");

	private final int value;
	private final String name;

	private BatteryType(int value, String name) {
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