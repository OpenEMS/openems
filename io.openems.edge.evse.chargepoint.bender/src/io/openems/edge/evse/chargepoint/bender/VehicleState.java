package io.openems.edge.evse.chargepoint.bender;

import io.openems.common.types.OptionsEnum;

public enum VehicleState implements OptionsEnum {
	UNDEFINED(-1, "Undefined", false, false), //
	STATE_A(1, "No EV connected to the EVSE", false, false), //
	STATE_B(2, "EV connected to the EVSE, but not ready for charging", false, true), //
	STATE_C(3, "Connected and ready for charging, ventilation is not required", true, true), //
	STATE_D(4, "Connected, ready for charging and ventilation is required", true, true), //
	STATE_E(5, "Electrical short to earth on the controller of the EVSE, no power supply", false, false) //
	;

	private final int value;
	private final String name;
	private final boolean isReadyForCharging;
	private final boolean isEvConnected;

	private VehicleState(int value, String name, boolean isReadyForCharging, boolean isEvConnected) {
		this.value = value;
		this.name = name;
		this.isReadyForCharging = isReadyForCharging;
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

	/**
	 * Interpretates if this {@link VehicleState} means that a
	 * {@link AbstractEvseChargePointBender} is ready for charging.
	 * 
	 * @return is ready for charging
	 */
	public boolean isReadyForCharging() {
		return this.isReadyForCharging;
	}

	/**
	 * Interpretates if this {@link VehicleState} means that a
	 * {@link AbstractEvseChargePointBender} has a ev connected.
	 * 
	 * @return is ev connected
	 */
	public boolean isEvConnected() {
		return this.isEvConnected;
	}

}
