package io.openems.edge.ess.generic.common.essprotection;

import java.time.Clock;

import com.google.common.annotations.VisibleForTesting;

import io.openems.common.timedata.Timeout;

public class EpForceChargeHandler {

	private static final int FORCE_MODE_DELAY_SECONDS = 60;

	private final Clock clock;
	private final Timeout waitForForceMode = Timeout.ofSeconds(FORCE_MODE_DELAY_SECONDS);
	private final EpForceChargeParams params;

	private State state = State.IDLE;
	private EpForceChargeVoltages limits;

	public EpForceChargeHandler(EpForceChargeParams params) {
		this(params, Clock.systemDefaultZone());
	}

	@VisibleForTesting
	public EpForceChargeHandler(EpForceChargeParams params, Clock clock) {
		this.params = params;
		this.clock = clock;
	}

	protected void update(int batteryVoltage, int dcMinVoltage) {
		this.limits = EpForceChargeVoltages.fromParams(dcMinVoltage, this.params);
		this.state = switch (this.state) {
		case IDLE -> this.handleIdleState(batteryVoltage);

		case WAIT_FOR_FORCE_MODE -> this.handleWaitForForceModeState(batteryVoltage);

		case FORCE_MODE -> this.handleForceModeState(batteryVoltage);

		case BLOCK_MODE -> this.handleBlockModeState(batteryVoltage);
		};
	}

	/**
	 * IDLE state handler.
	 * 
	 * <p>
	 * Waits for battery voltage to drop below start threshold.
	 * 
	 * @param batteryVoltage current battery voltage
	 * @return next state based on battery voltage
	 */
	protected State handleIdleState(int batteryVoltage) {
		if (batteryVoltage <= this.limits.startChargeVoltage) {
			// Battery dropped below threshold, enter waiting phase

			this.waitForForceMode.start(this.clock);
			return State.WAIT_FOR_FORCE_MODE;
		}
		return State.IDLE;
	}

	/**
	 * WAIT_FOR_FORCE_MODE state handler.
	 * 
	 * <p>
	 * Waits for a timeout before activating force charge. This prevents rapid
	 * on/off switching.
	 * 
	 * @param batteryVoltage current battery voltage
	 * @return next state based on battery voltage
	 */
	protected State handleWaitForForceModeState(int batteryVoltage) {

		if (batteryVoltage > this.limits.startChargeVoltage) {
			// Voltage recovered, go back to idle
			return State.IDLE;
		}

		if (this.waitForForceMode.elapsed(this.clock)) {
			return State.FORCE_MODE;
		}

		return State.WAIT_FOR_FORCE_MODE;
	}

	/**
	 * FORCE_MODE state handler.
	 * 
	 * <p>
	 * Force-Charge is active, loading with full current.
	 * 
	 * 
	 * @param batteryVoltage current battery voltage
	 * @return next state based on battery voltage
	 */
	protected State handleForceModeState(int batteryVoltage) {
		if (batteryVoltage >= this.limits.chargeBelowVoltage) {
			// Battery charged to safe level, enter block mode
			// (discharge will be blocked until even higher voltage)
			return State.BLOCK_MODE;
		}
		// Continue force charging
		return State.FORCE_MODE;
	}

	/**
	 * BLOCK_MODE state handler.
	 * 
	 * <p>
	 * Force-Charge is inactive, but discharge is blocked until voltage reaches
	 * blockDischargeVoltage.
	 * 
	 * @param batteryVoltage current battery voltage
	 * @return next state based on battery voltage
	 */
	protected State handleBlockModeState(int batteryVoltage) {
		if (batteryVoltage <= this.limits.startChargeVoltage) {
			// Battery dropped again, restart force charge
			return State.FORCE_MODE;
		}

		if (batteryVoltage <= this.limits.blockDischargeVoltage) {
			// Still in safe range, keep discharge blocked
			return State.BLOCK_MODE;
		}

		// Battery at safe voltage, allow discharge
		return State.IDLE;
	}

	/**
	 * Gets current state.
	 * 
	 * @return current state
	 */
	public State getState() {
		return this.state;
	}

	public enum State {
		IDLE, //
		WAIT_FOR_FORCE_MODE, //
		FORCE_MODE, //
		BLOCK_MODE //
	}

	protected record EpForceChargeParams(int startChargeVoltageOffset, // [V] Force-Charge Entry
			int chargeBelowVoltageOffset, // [V] Force-Charge Exit
			int blockDischargeVoltageOffset // [V] Block further discharge
	) {

		protected static EpForceChargeParams create(int startChargeVoltageOffset, int chargeBelowVoltageOffset,
				int blockDischargeVoltageOffset) {
			return new EpForceChargeParams(startChargeVoltageOffset, chargeBelowVoltageOffset,
					blockDischargeVoltageOffset);
		}
	}

	private record EpForceChargeVoltages(int startChargeVoltage, int chargeBelowVoltage, int blockDischargeVoltage) {
		public static EpForceChargeVoltages fromParams(int dcMinVoltage, EpForceChargeParams params) {
			return new EpForceChargeVoltages(dcMinVoltage - params.startChargeVoltageOffset(),
					dcMinVoltage + params.chargeBelowVoltageOffset(),
					dcMinVoltage + params.blockDischargeVoltageOffset());
		}
	}
}
