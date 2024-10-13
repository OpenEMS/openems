package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.EnumUtils;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;

public class DeactivationTimeHandler extends StateHandler<State, Context> {

	private static final int ZERO_WATT_POWER = 0; //[0 W]
	protected Instant deactivationStateStartTime;

	private static enum SubState {
		HOLD_DEACTIVATION, //
		FINISH_DEACTIVATION_DURATION
	}

	protected static record DeactivationTimeState(SubState subState, Instant lastChange) {
	}

	protected DeactivationTimeState deactivationTimeState;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		context.ess.setActivePowerEquals(ZERO_WATT_POWER);
		this.deactivationStateStartTime = Instant.now(context.clock);
		this.deactivationTimeState = new DeactivationTimeState(SubState.HOLD_DEACTIVATION, Instant.now(context.clock));
	}

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		var nextSubState = this.getNextSubState(context);
		if (nextSubState != this.deactivationTimeState.subState) {
			this.deactivationTimeState = new DeactivationTimeState(nextSubState, Instant.now(context.clock));
		}
		if (nextSubState == SubState.FINISH_DEACTIVATION_DURATION) {
			return State.BUFFERED_TIME_BEFORE_RECOVERY;
		}
		return State.DEACTIVATION_TIME;
	}

	private SubState getNextSubState(Context context) throws OpenemsNamedException {
		return switch (this.deactivationTimeState.subState) {

		case HOLD_DEACTIVATION -> {
			context.ess.setActivePowerEquals(ZERO_WATT_POWER);
			var deactivationDurationExpiration = this.calculateDeactivationDurationExpiration(context);
			if (deactivationDurationExpiration >= context.activationRunTime.getValue()) {
				yield SubState.FINISH_DEACTIVATION_DURATION;
			}
			yield SubState.HOLD_DEACTIVATION;
		}
		case FINISH_DEACTIVATION_DURATION -> SubState.FINISH_DEACTIVATION_DURATION;
		};
	}

	/**
	 * Calculates the expiration duration for the deactivation state. The expiration
	 * duration is the time elapsed between the deactivation state start time and
	 * the current time, measured in milliseconds.
	 * 
	 * @param context the Context
	 * @return The expiration duration in milliseconds.
	 */
	private long calculateDeactivationDurationExpiration(Context context) {
		return Duration.between(//
				this.deactivationStateStartTime, //
				Instant.now(context.clock))//
				.toMillis();
	}

	@Override
	protected String debugLog() {
		return State.DEACTIVATION_TIME.asCamelCase() + "-"
				+ EnumUtils.nameAsCamelCase(this.deactivationTimeState.subState());
	}

}
