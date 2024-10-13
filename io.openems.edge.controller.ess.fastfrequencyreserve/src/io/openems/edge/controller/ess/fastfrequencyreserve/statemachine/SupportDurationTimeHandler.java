package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;

public class SupportDurationTimeHandler extends StateHandler<State, Context> {

	protected LocalDateTime supportDurationStartTime;

	private static enum SubState {
		HOLD_SUPPORT, //
		FINISH_SUPPORT_DURATION
	}

	protected static record SupportDurationTimeState(SubState subState, Instant lastChange) {
	}

	protected SupportDurationTimeState supportDurationTimeState;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		context.ess.setActivePowerEquals(context.dischargePower);
		this.supportDurationStartTime = LocalDateTime.now(context.clock);
		this.supportDurationTimeState = new SupportDurationTimeState(SubState.HOLD_SUPPORT, Instant.now(context.clock));
	}

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var nextSubState = this.getNextSubState(context);
		if (nextSubState != this.supportDurationTimeState.subState) {
			this.supportDurationTimeState = new SupportDurationTimeState(nextSubState, Instant.now(context.clock));
		}
		if (nextSubState == SubState.FINISH_SUPPORT_DURATION) {
			return State.DEACTIVATION_TIME;
		}
		return State.SUPPORT_DURATION;
	}

	private SubState getNextSubState(Context context) throws OpenemsNamedException {
		return switch (this.supportDurationTimeState.subState) {
		case HOLD_SUPPORT -> {
			context.ess.setActivePowerEquals(context.dischargePower);
			var supportDurationExpiration = this.calculateSupportDurationExpiration(context);
			if (supportDurationExpiration >= context.supportDuration.getValue()) {
				yield SubState.FINISH_SUPPORT_DURATION;
			}
			yield SubState.HOLD_SUPPORT;
		}
		case FINISH_SUPPORT_DURATION -> SubState.FINISH_SUPPORT_DURATION;
		};
	}

	private long calculateSupportDurationExpiration(Context context) {
		return Duration.between(//
				this.supportDurationStartTime, //
				LocalDateTime.now(context.clock))//
				.getSeconds();
	}
}
