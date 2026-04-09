package io.openems.edge.controller.ess.fixstateofcharge.statemachine;

import static io.openems.edge.controller.ess.fixstateofcharge.statemachine.ReferenceCycleTarget.CHARGE_TO_HUNDRED;
import static io.openems.edge.controller.ess.fixstateofcharge.statemachine.ReferenceCycleTarget.DISCHARGE_TO_ZERO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.StateMachine.State;

/**
 * Handler for REFERENCE_CYCLE state.
 *
 * <p>
 * The reference cycle is an optional initialization phase that runs only when
 * enabled (currently only for
 * {@link io.openems.edge.controller.ess.fixstateofcharge.ControllerEssPrepareBatteryExtensionImpl}).
 * When enabled, it runs once after NOT_STARTED state to verify battery
 * performance. The handler executes the reference cycle logic repeatedly until
 * it completes (returns true), then transitions to the appropriate SoC state
 * and never returns to REFERENCE_CYCLE.
 *
 * <p>
 * <b>Reference Cycle Sequence:</b>
 * <ol>
 * <li>Determine reference target based on initial SoC:
 * <ul>
 * <li>SoC ≥ 70 → target 100 (discharge to verify battery maximum capacity)</li>
 * <li>SoC < 70 → target 0 (charge to verify battery minimum capacity)</li>
 * </ul>
 * </li>
 * <li>Charge or discharge at 50% of capacity (0.5C rate) to reach target</li>
 * <li>Wait 30 minutes at target SoC</li>
 * <li>Transition to normal charging/discharging toward the actual target
 * SoC</li>
 * </ol>
 */
public class ReferenceCycleHandler extends StateHandler<State, Context> {

	private static final Logger log = LoggerFactory.getLogger(ReferenceCycleHandler.class);
	public static final int SOC_0 = 0;
	public static final int SOC_70 = 70;
	public static final double RAMP_5_PERCENT = 0.05;

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		this.applyReferenceRampPower(context);

		if (!this.advanceAndCheckIfCompleted(context)) {
			return State.REFERENCE_CYCLE;
		}

		return transitionToNextState(context);
	}

	private void applyReferenceRampPower(Context context) {
		context.setRampPower(context.maxApparentPower * RAMP_5_PERCENT);
	}

	private boolean advanceAndCheckIfCompleted(Context context) {
		var referenceTarget = this.ensureReferenceTargetInitialized(context);

		if (referenceTarget.isReached(context.soc)) {
			return this.isReferencePauseComplete(context);
		}

		this.applyReferencePower(context, referenceTarget);
		return false;
	}

	private ReferenceCycleTarget ensureReferenceTargetInitialized(Context context) {
		var referenceTarget = context.getParent().getReferenceCycleTarget();
		if (referenceTarget == null) {
			referenceTarget = context.soc >= SOC_70 ? CHARGE_TO_HUNDRED : DISCHARGE_TO_ZERO;
			context.getParent().setReferenceCycleTarget(referenceTarget);
		}
		return referenceTarget;
	}

	private void applyReferencePower(Context context, ReferenceCycleTarget referenceTarget) {
		context.getParent().clearReferenceCyclePauseStart();

		var capacityWh = context.getParent().getEss().getCapacity().orElse(null);
		if (capacityWh == null || capacityWh <= SOC_0) {
			log.warn("Capacity is not available or invalid ({}), using max apparent power for reference cycle",
					capacityWh);
		}
		var maxReferencePower = ReferenceCycleUtils.calculateMaxReferencePower(context.maxApparentPower, capacityWh);

		final var targetPower = switch (referenceTarget) {
			case CHARGE_TO_HUNDRED -> -maxReferencePower;
			case DISCHARGE_TO_ZERO -> +maxReferencePower;
		};

		context.setTargetPower(targetPower);
	}

	private boolean isReferencePauseComplete(Context context) {
		if (this.isPauseNotStarted(context)) {
			this.startPauseTimer(context);
			context.setTargetPower(SOC_0);
			return false;
		}

		if (this.isPauseStillRunning(context)) {
			context.setTargetPower(SOC_0);
			return false;
		}

		return true;
	}

	private boolean isPauseNotStarted(Context context) {
		return context.getParent().getReferenceCyclePauseStartMs() == null;
	}

	private void startPauseTimer(Context context) {
		context.getParent().setReferenceCyclePauseStartMs(context.clock.millis());
	}

	private boolean isPauseStillRunning(Context context) {
		var pauseStartMs = context.getParent().getReferenceCyclePauseStartMs();
		var nowMs = context.clock.millis();
		var pauseDurationMs = nowMs - pauseStartMs;
		return pauseDurationMs < ReferenceCycleUtils.REFERENCE_CYCLE_PAUSE_MS;
	}


	private static State transitionToNextState(Context context) {
		return Context.getSocState(context.soc, context.targetSoc);
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		context.getParent().clearReferenceCycleTarget();
		context.getParent().clearReferenceCyclePauseStart();
		super.onExit(context);
	}
}
