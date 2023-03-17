package io.openems.edge.solaredge.enums;

import io.openems.common.types.OptionsEnum;

public  enum ChargeDischargeMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SE_CHARGE_POLICY_NONE(0, "Disabled"), //
	SE_CHARGE_POLICY_EXCESS_PV(1, "Only PV excess power not going to AC is used for charging the battery. "),
	SE_CHARGE_POLICY_FIRST(2, "Charge from PV first, before producing power to the AC."),
	SE_CHARGE_POLICY_PV_AC(3, "Charge from PV+AC according to the max battery power"),
	SE_CHARGE_POLICY_MAX_EXPORT(4, "Maximize export – discharge battery to meet max inverter AC limit"),
	SE_CHARGE_POLICY_DISCHARGE(5, "Discharge to meet loads consumption. Discharging to the grid is not allowed"),
	// Value 6 is not defined
	SE_CHARGE_POLICY_MAX_SELF_CONSUMPTION(7, "Maximize self-consumption "); //

	private final int value;
	private final String name;

	private ChargeDischargeMode(int value, String name) {
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