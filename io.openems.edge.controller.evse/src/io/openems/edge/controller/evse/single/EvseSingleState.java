package io.openems.edge.controller.evse.single;

import io.openems.common.types.OptionsEnum;

/**
 * Represents the possible states of the EVSE (Electric Vehicle Supply
 * Equipment) during the lifecycle of a charging session.
 */
public enum EvseSingleState implements io.openems.edge.common.statemachine.State<EvseSingleState>, OptionsEnum {
	UNDEFINED(-1), //

	/**
	 * EV is not physically connected to the charge point.
	 */
	EV_NOT_CONNECTED(10), //
	/**
	 * Wallbox is not ready to charge.
	 */
	NOT_READY(11), //

	/**
	 * EV is physically connected but not yet charging.
	 */
	EV_CONNECTED(20), //
	/**
	 * EV is connected, but RFID authentication is required before charging can
	 * start.
	 */
	// EV_CONNECTED_AWAITING_RFID(21), //

	/**
	 * Charge was paused because there was not enough SURPLUS.
	 */
	CHARGE_PAUSED(40), //

	/**
	 * Charge was disabled because the configured mode is OFF.
	 */
	CHARGE_DISABLED(41), //

	/**
	 * Charging has been initiated. EVSE allows current, waiting for EV to start
	 * drawing power.
	 */
	CHARGE_RESUMING(42), //

	/**
	 * Forcing initial Charging - even if no PV surplus is available. Used for a
	 * limited time (e.g. to wake up the EV).
	 */
	// CHARGING_FORCED_INITIAL(31), //

	/**
	 * Charging is paused for unspecified reasons. This is a generic parent state
	 * for paused charging.
	 */
	// CHARGING_PAUSED(40), //
	/**
	 * Charging is paused by user intervention.
	 */
	// CHARGING_PAUSED_BY_USER(41), //
	/**
	 * Charging is paused because insufficient PV surplus power is available.
	 */
	// CHARGING_PAUSED_AWAITING_PV(42), //
	/**
	 * Charging is paused because the system is waiting for a cheap time-of-use
	 * (TOU) tariff window to begin.
	 */
	// CHARGING_PAUSED_AWAITING_TOU(43), //
	/**
	 * Charging has been allowed to resume, but the EV has not yet started drawing
	 * power again.
	 */
	// CHARGING_PAUSED_RESUMING(48), //
	/**
	 * An attempt to resume charging after a pause has failed (e.g. EV did not
	 * restart charging).
	 */
	// CHARGING_PAUSED_RESUMING_FAILED(49), //

	/**
	 * Regular charging is ongoing. EV is drawing current according to configured
	 * limits and conditions.
	 */
	CHARGING(50), //
	/**
	 * Charging is kept active by hysteresis during low PV surplus power period.
	 */
	// CHARGING_PV_HYSTERESIS(51), //

	/**
	 * Charging finished by EV. EV is not drawing power even if it would be allowed
	 * to.
	 */
	FINISHED_EV_STOP(60), //
	/**
	 * Charging is finished by OpenEMS because the configured `EnergySessionLimit`
	 * was reached.
	 */
	FINISHED_ENERGY_SESSION_LIMIT(61), //
	/**
	 * Charging finished because the EV battery reached full state-of-charge.
	 */
	// FINISHED_FULL(62), // NOTE: requires EV SoC

	/**
	 * A generic error state. Details may be provided by sub-error states.
	 */
	// ERROR(80), //
	/**
	 * Error related to the charge point (EVSE hardware/software fault).
	 */
	// ERROR_CHARGE_POINT(81), //
	/**
	 * Error related to the EV (e.g. communication error, EV refused to charge).
	 */
	// ERROR_EV(82), //

	PHASE_SWITCH_TO_THREE_PHASE(91), //
	PHASE_SWITCH_TO_SINGLE_PHASE(92), //
	;

	private final int value;

	private EvseSingleState(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name();
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

	@Override
	public EvseSingleState[] getStates() {
		return EvseSingleState.values();
	}
}