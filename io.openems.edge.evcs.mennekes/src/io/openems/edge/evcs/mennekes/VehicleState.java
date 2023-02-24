package io.openems.edge.evcs.mennekes;

import io.openems.common.types.OptionsEnum;

public enum VehicleState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STATE_A(1, "No EV connected to the EVSE "), //
	STATE_B(2, "EV connected to the EVSE, but not ready for charging "), //
	STATE_C(3, "Connected and ready for charging, ventilation is not required "), //
	STATE_D(4, "Connected, ready for charging and ventilation is required "), //
	STATE_E(5, "Electrical short to earth on the controller of the EVSE, no power supply") //
	;

	private final int value;
	private final String name;

	private VehicleState(int value, String name) {
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
