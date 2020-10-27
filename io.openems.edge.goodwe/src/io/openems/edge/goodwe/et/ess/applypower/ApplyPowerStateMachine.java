package io.openems.edge.goodwe.et.ess.applypower;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.goodwe.et.ess.enums.GoodweType;

public class ApplyPowerStateMachine extends AbstractStateMachine<ApplyPowerStateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		UNDEFINED(-1), //

		READ_ONLY(10), //

		/*
		 * GoodWe ET handling
		 */
		ET_FULL_POSITIVE_DISCHARGE(20), //
		ET_FULL_POSITIVE_CURTAIL(21), //
		ET_FULL_NEGATIVE_CURTAIL(21), //

		ET_EMPTY_POSITIVE_PV(30), //
		ET_EMPTY_NEGATIVE_CHARGE(31), //

		ET_INBETWEEN_POSITIVE_DISCHARGE(41), //
		ET_INBETWEEN_POSITIVE_CHARGE(42), //
		ET_INBETWEEN_NEGATIVE_CHARGE(43), //

		/*
		 * GoodWe BT handling
		 */
		BT_CHARGE(50), //
		BT_DISCHARGE(51), //
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

		case ET_EMPTY_NEGATIVE_CHARGE:
			return new EtEmptyNegativeChargeHandler();
		case ET_EMPTY_POSITIVE_PV:
			return new EtEmptyPositivePvHandler();
		case ET_FULL_NEGATIVE_CURTAIL:
			return new EtFullNegativeCurtailHandler();
		case ET_FULL_POSITIVE_CURTAIL:
			return new EtFullPositiveCurtailHandler();
		case ET_FULL_POSITIVE_DISCHARGE:
			return new EtFullPositiveDischargeHandler();
		case ET_INBETWEEN_NEGATIVE_CHARGE:
			return new EtInbetweenNegativeChargeHandler();
		case ET_INBETWEEN_POSITIVE_CHARGE:
			return new EtInbetweenPositiveChargeHandler();
		case ET_INBETWEEN_POSITIVE_DISCHARGE:
			return new EtInbetweenPositiveDischargeHandler();

		case BT_CHARGE:
			return new BtChargeHandler();
		case BT_DISCHARGE:
			return new BtDischargeHandler();
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
	public static State evaluateState(GoodweType goodweType, boolean readOnlyMode, int pvProduction, int soc,
			int activePowerSetPoint) {
		if (readOnlyMode) {
			// Read-Only-Mode: fall-back to internal self-consumption optimization
			return State.READ_ONLY;

		} else {
			switch (goodweType) {
			case GOODWE_10K_BT:
				if (activePowerSetPoint > 0) {
					// Set-Point is positive
					return State.BT_DISCHARGE;
				} else {
					// Set-Point is negative or zero
					return State.BT_CHARGE;
				}

			case GOODWE_10K_ET:
				if (soc > 99) {
					// battery is full
					if (activePowerSetPoint > 0) {
						// Set-Point is positive -> take power either from pv or battery
						if (activePowerSetPoint > pvProduction) {
							return State.ET_FULL_POSITIVE_DISCHARGE;

						} else {
							return State.ET_FULL_POSITIVE_CURTAIL;
						}
					} else {
						return State.ET_FULL_NEGATIVE_CURTAIL;
					}

				} else if (soc < 1) {
					// battery is empty
					// TODO define when battery is empty
					if (activePowerSetPoint > 0) {
						return State.ET_EMPTY_POSITIVE_PV;

					} else {
						// Set-Point is negative or zero -> 'charge' from pv production and grid
						return State.ET_EMPTY_NEGATIVE_CHARGE;
					}

				} else {
					// battery is neither empty nor full
					if (activePowerSetPoint > 0) {
						// Set-Point is positive
						if (activePowerSetPoint > pvProduction) {
							return State.ET_INBETWEEN_POSITIVE_DISCHARGE;

						} else {
							return State.ET_INBETWEEN_POSITIVE_CHARGE;

						}

					} else {
						// Set-Point is negative or zero
						return State.ET_INBETWEEN_NEGATIVE_CHARGE;
					}
				}

			case UNDEFINED:
				// This should not stay for long on bootup...
				return State.UNDEFINED;
			}

		}

		// should not come here
		return State.UNDEFINED;
	}
}