package io.openems.edge.controller.ess.standby.statemachine;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.standby.statemachine.StateMachine.State;

public class DischargeHandler extends StateHandler<State, Context> {

	private final DischargePowerLimitHandler dischargePowerLimitHandler = new DischargePowerLimitHandler();

	private final EvaluateNextStateHandler evaluateNextStateHandler = new EvaluateNextStateHandler();

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.dischargePowerLimitHandler.initialize();
		this.evaluateNextStateHandler.initialize();
	}

	@Override
	public State runAndGetNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {
		// Discharge without feeding to grid (just like in Balancing Controller)
		int gridPower = context.sum.getGridActivePower().getOrError();
		int essPower = context.ess.getActivePower().getOrError();
		var setPower = gridPower + essPower;
		if (setPower < 0) {
			// do not charge
			setPower = 0;
		}

		// Apply power limit
		setPower = this.dischargePowerLimitHandler.applyPowerLimit(context, setPower);

		// Apply power constraint
		context.ess.setActivePowerEqualsWithPid(setPower);

		// Evaluate next state
		return this.evaluateNextStateHandler.getNextState(context);
	}

	/**
	 * Handles the limitation of Discharge Power according to the specification.
	 *
	 * <ul>
	 * <li>discharge like in Balancing (keep grid at 0)
	 * <li>if discharge power > 70 %
	 * ({@link DischargePowerLimitHandler#HIGH_POWER_THRESHOLD}) of maxApparentPower
	 * for more than 10 minutes in a row -> limit discharge power to 50 %
	 * {@link DischargePowerLimitHandler#POWER_LIMIT_FACTOR}
	 * <li>make sure to never discharge to the grid
	 * </ul>
	 *
	 */
	private static class DischargePowerLimitHandler {
		/**
		 * If 'power' > 'maxApparentPower' x HIGH_POWER_THRESHOLD it is considered 'high
		 * discharge power'.
		 */
		private static final double HIGH_POWER_THRESHOLD = 0.7;

		/**
		 * 'high discharge power' is active (without interruptions) since
		 * 'highDischargePowerSince'.
		 */
		private Instant highDischargePowerSince = null;

		/**
		 * If duration of 'high discharge power' exceeds MAX_HIGH_POWER_MINUTES, power
		 * gets limited.
		 */
		private static final int MAX_HIGH_POWER_MINUTES = 10;

		/**
		 * Keeps the information if discharge power should be limited after
		 * MAX_HIGH_POWER_MINUTES exceeded.
		 */
		private boolean isDischargePowerLimited = false;

		/*
		 * If isDischargePowerLimited is active, the discharge power is limited to
		 * 'maxApparentPower' x POWER_LIMIT_FACTOR.
		 */
		private static final float POWER_LIMIT_FACTOR = 0.5f;

		/**
		 * Initialize variables.
		 */
		protected void initialize() {
			this.highDischargePowerSince = null;
			this.isDischargePowerLimited = false;
		}

		/**
		 * Applies the power limit.
		 *
		 * @param context  the {@link Context}
		 * @param setPower the initial power setpoint
		 * @return the new limit
		 * @throws InvalidValueException    on error
		 * @throws IllegalArgumentException on error
		 */
		protected int applyPowerLimit(Context context, int setPower)
				throws InvalidValueException, IllegalArgumentException {
			// If discharge power > HIGH_POWER_THRESHOLD (e.g. 70 %) of MaxApparantPower for
			// more than MAX_HIGH_POWER_MINUTES (e.g. 10 minutes) in a row -> limit power to
			// to 50 %
			int maxApparentPower = context.ess.getMaxApparentPower().getOrError();
			if (setPower > maxApparentPower * HIGH_POWER_THRESHOLD) {
				var now = Instant.now(context.clock);
				if (this.highDischargePowerSince == null) {
					this.highDischargePowerSince = now;
				}
				if (Duration.between(this.highDischargePowerSince, now).toMinutes() >= MAX_HIGH_POWER_MINUTES) {
					this.isDischargePowerLimited = true;
				}
			}

			// Apply power limitation
			if (this.isDischargePowerLimited) {
				var limit = Math.round(maxApparentPower * POWER_LIMIT_FACTOR);
				if (setPower > limit) {
					setPower = limit;
				}
			}
			return setPower;
		}
	}

	/**
	 * Wraps the evaluation of the next State after DISCHARGE according to the
	 * specification.
	 *
	 * <ul>
	 * <li>Production is higher than Consumption for more than 1 minute.
	 * ({@link EvaluateNextStateHandler#MAX_PRODUCTION_HIGHER_THAN_CONSUMPTION_MINUTES})
	 * <li>Latest at 12 o'clock ({@link EvaluateNextStateHandler#SWITCH_LATEST_AT})
	 * </ul>
	 */
	private static class EvaluateNextStateHandler {

		/**
		 * Switch to next State latest at SWITCH_LATEST_AT.
		 */
		private static final LocalTime SWITCH_LATEST_AT = LocalTime.NOON;

		/**
		 * If Production is higher than Consumption for
		 * MAX_PRODUCTION_HIGHER_THAN_CONSUMPTION_MINUTES, switch to next State.
		 */
		private static final int MAX_PRODUCTION_HIGHER_THAN_CONSUMPTION_MINUTES = 1;

		/**
		 * Production Power is higher than Consumption since.
		 */
		private Instant productionHigherThanConsumptionSince = null;

		/**
		 * Initialize variables.
		 */
		protected void initialize() {
			this.productionHigherThanConsumptionSince = null;
		}

		/**
		 * Gets the next State.
		 *
		 * @param context the {@link Context}
		 * @return the next {@link State}.
		 * @throws IllegalArgumentException on error
		 * @throws InvalidValueException    on error
		 */
		public State getNextState(Context context) throws InvalidValueException, IllegalArgumentException {
			if (LocalTime.now(context.clock).isAfter(SWITCH_LATEST_AT)) {
				return State.SLOW_CHARGE_1;
			}

			int production = context.sum.getProductionActivePower().orElse(0);
			int consumption = context.sum.getConsumptionActivePower().orElse(0);
			if (production > consumption) {
				var now = Instant.now(context.clock);
				if (this.productionHigherThanConsumptionSince == null) {
					this.productionHigherThanConsumptionSince = now;
				}
				if (Duration.between(this.productionHigherThanConsumptionSince, now)
						.toMinutes() >= MAX_PRODUCTION_HIGHER_THAN_CONSUMPTION_MINUTES) {
					// time passed
					return State.SLOW_CHARGE_1;
				}
			} else {
				this.productionHigherThanConsumptionSince = null;
			}

			// stay in current State
			return State.DISCHARGE;
		}
	}
}
