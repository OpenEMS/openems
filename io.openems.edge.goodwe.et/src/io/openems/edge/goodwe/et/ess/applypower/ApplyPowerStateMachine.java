package io.openems.edge.goodwe.et.ess.applypower;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class ApplyPowerStateMachine extends AbstractStateMachine<ApplyPowerStateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		UNDEFINED(-1), //

		READ_ONLY(10), //

		FULL_POSITIVE_DISCHARGE(20), //
		FULL_POSITIVE_CURTAIL(21), //
		FULL_NEGATIVE_CURTAIL(21), //

		EMPTY_POSITIVE_PV(30), //
		EMPTY_NEGATIVE_CHARGE(31), //

		INBETWEEN_POSITIVE_DISCHARGE(41), //
		INBETWEEN_POSITIVE_CHARGE(42), //
		INBETWEEN_NEGATIVE_CHARGE(43) //
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

	public ApplyPowerStateMachine(State initialState) {
		super(initialState);
	}

	@Override
	public StateHandler<State, Context> getStateHandler(State state) {
		switch (state) {
		case UNDEFINED:
			return new UndefinedHandler();
		case READ_ONLY:
			return new ReadOnlyHandler();
		case EMPTY_NEGATIVE_CHARGE:
			return new EmptyNegativeChargeHandler();
		case EMPTY_POSITIVE_PV:
			return new EmptyPositivePvHandler();
		case FULL_NEGATIVE_CURTAIL:
			return new FullNegativeCurtailHandler();
		case FULL_POSITIVE_CURTAIL:
			return new FullPositiveCurtailHandler();
		case FULL_POSITIVE_DISCHARGE:
			return new FullPositiveDischargeHandler();
		case INBETWEEN_NEGATIVE_CHARGE:
			return new InBetweenNegativeChargeHandler();
		case INBETWEEN_POSITIVE_CHARGE:
			return new InBetweenPositiveChargeHandler();
		case INBETWEEN_POSITIVE_DISCHARGE:
			return new InBetweenPositiveDischargeHandler();
		default:
			break;
		}
		throw new IllegalArgumentException("Unknown State [" + state + "]");
	}

	/**
	 * Evaluates the State we are currently in.
	 * 
	 * @param readOnlyMode
	 * @param pvProduction
	 * @param soc
	 * @param activePowerSetPoint
	 * @return
	 */
	public static State evaluateState(boolean readOnlyMode, int pvProduction, int soc, int activePowerSetPoint) {
		if (readOnlyMode) {
			// Read-Only-Mode: fall-back to internal self-consumption optimization
			return State.READ_ONLY;

		} else {
			if (soc > 99) {
				// battery is full
				if (activePowerSetPoint > 0) {
					// Set-Point is positive -> take power either from pv or battery
					if (activePowerSetPoint > pvProduction) {
						return State.FULL_POSITIVE_DISCHARGE;

					} else {
						return State.FULL_POSITIVE_CURTAIL;
					}
				} else {
					return State.FULL_NEGATIVE_CURTAIL;
				}

			} else if (soc < 1) {
				// battery is empty
				// TODO define when battery is empty
				if (activePowerSetPoint > 0) {
					return State.EMPTY_POSITIVE_PV;

				} else {
					// Set-Point is negative or zero -> 'charge' from pv production and grid
					return State.EMPTY_NEGATIVE_CHARGE;
				}

			} else {
				// battery is neither empty nor full
				if (activePowerSetPoint > 0) {
					// Set-Point is positive
					if (activePowerSetPoint > pvProduction) {
						return State.INBETWEEN_POSITIVE_DISCHARGE;

					} else {
						return State.INBETWEEN_POSITIVE_CHARGE;

					}

				} else {
					// Set-Point is negative or zero
					return State.INBETWEEN_NEGATIVE_CHARGE;
				}
			}
		}
	}
}