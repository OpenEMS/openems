package io.openems.edge.evse.chargepoint.hardybarth;

import io.openems.common.types.OptionsEnum;

public enum ChargePointStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	A(0, "No vehicle connected (A)"), //
	B(1, "Vehicle connected, not charging (B)"), //
	C(2, "Vehicle connected and charging (C)"), //
	D(3, "Charging with ventilation required (D)"), //
	E(4, "Error – communication problem (E)"), //
	F(5, "Error – serious fault (F)"); //

	private final int value;
	private final String name;

	private ChargePointStatus(int value, String name) {
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
	public ChargePointStatus getUndefined() {
		return UNDEFINED;
	}
}
