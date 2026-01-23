package io.openems.edge.controller.evse.single.statemachine;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	/**
	 * Represents the possible states of the EVSE (Electric Vehicle Supply
	 * Equipment) during the lifecycle of a charging session.
	 */
	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		UNDEFINED(-1), //

		/**
		 * EV is not physically connected to the charge point.
		 */
		EV_NOT_CONNECTED(10), //

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
		 * Charging has been initiated. EVSE allows current, waiting for EV to start
		 * drawing power.
		 */
		// CHARGING_INITIATED(30), //
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

		private State(int value) {
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
		public State[] getStates() {
			return State.values();
		}
	}

	public StateMachine(State initialState) {
		super(initialState);
	}

	@Override
	public StateHandler<State, Context> getStateHandler(State state) {
		return switch (state) {
		case UNDEFINED -> new UndefinedHandler();
		case EV_NOT_CONNECTED -> new EvNotConnectedHandler();
		case EV_CONNECTED -> new EvConnectedHandler();
		case CHARGING -> new ChargingHandler();
		case FINISHED_EV_STOP -> new FinishedEvStopHandler();
		case FINISHED_ENERGY_SESSION_LIMIT -> new FinishedEnergySessionLimitHandler();
		case PHASE_SWITCH_TO_THREE_PHASE -> new PhaseSwitchHandler.ToThreePhase();
		case PHASE_SWITCH_TO_SINGLE_PHASE -> new PhaseSwitchHandler.ToSinglePhase();
		};
	}
}