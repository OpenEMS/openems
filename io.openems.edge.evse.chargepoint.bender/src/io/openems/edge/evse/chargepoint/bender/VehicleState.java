package io.openems.edge.evse.chargepoint.bender;

import io.openems.common.types.OptionsEnum;

public enum VehicleState implements OptionsEnum {
	UNDEFINED(-1, "Undefined", false), //
	STATE_A(1, "No EV connected to the EVSE", false), //
	STATE_B(2, "EV connected to the EVSE, but not ready for charging", true), //
	STATE_C(3, "Connected and ready for charging, ventilation is not required", true), //
	STATE_D(4, "Connected, ready for charging and ventilation is required", true), //
	STATE_E(5, "Electrical short to earth on the controller of the EVSE, no power supply", false) //
	;
	
	public final boolean isEvConnected;

	private final int value;
	private final String name;

	private VehicleState(int value, String name, boolean isEvConnected) {
		this.value = value;
		this.name = name;
		this.isEvConnected = isEvConnected;
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
