package io.openems.edge.evse.chargepoint.bender;

import io.openems.common.types.OptionsEnum;

public enum OcppState implements OptionsEnum {
	UNDEFINED(0, "Undefined", false), //
	AVAILABLE(1, "Available", true), //
	PREPARING(2, "Preparing", true), //
	CHARGING(3, "Charging", true), //
	/**
	 * State is entered when hems limit of 0 is set in the modbus register.
	 */
	SUSPENDED_EVSE(4, "SuspendedEVSE", true), //
	/**
	 * Note: SUSPENDED_EV is considered readyForCharging = true intentionally.
	 * This follows the same logic as the KEBA implementation, where a suspended
	 * charging session (paused by the vehicle) is still treated as operational
	 * and capable of resuming charging without requiring a new session.
	 * (KEBA State NOT_READY_FOR_CHARGING -> true)
	 */
	SUSPENDED_EV(5, "SuspendedEV", true), //
	FINISHING(6, "Finishing", false), //
	RESERVED(7, "Reserved", false), //
	UNAVAILABLE(8, "Unavailable", false), //
	FAULTED(9, "Faulted", false); //

	public final boolean isReadyForCharging;

	private final int value;
	private final String name;

	private OcppState(int value, String name, boolean isReadyForCharging) {
		this.value = value;
		this.name = name;
		this.isReadyForCharging = isReadyForCharging;
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
