package io.openems.edge.controller.evcs;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.evcs.api.ChargeMode;

public enum SmartMode implements OptionsEnum {
	ZERO(0, "Zero", null, null), //
	// TODO SURPLUS_PV without min-charge power and only if there is predicted
	// surplus power
	// SURPLUS_PV(1, "Surplus PV with Min charge power", ChargeMode.EXCESS_POWER,
	// Priority.CAR), //
	FORCE(3, "Force charge", ChargeMode.FORCE_CHARGE, null) //
	;

	private final int value;
	private final String name;

	public final ChargeMode chargeMode;
	public final Priority priority;

	private SmartMode(int value, String name, ChargeMode chargeMode, Priority priority) {
		this.value = value;
		this.name = name;
		this.chargeMode = chargeMode;
		this.priority = priority;
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
		return ZERO;
	}
}