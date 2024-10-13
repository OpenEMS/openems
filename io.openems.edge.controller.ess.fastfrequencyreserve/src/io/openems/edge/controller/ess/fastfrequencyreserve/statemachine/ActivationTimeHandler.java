package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.EnumUtils;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;

public class ActivationTimeHandler extends StateHandler<State, Context> {

	private static final int ZERO_WATT_POWER = 0; // [0 W]

	protected Instant dipDetectedStartTime;
	protected ActivationTimeState activationTimeState;

	private static enum SubState {
		INSIDE_TIME_FRAME, //
		HANDLE_WAITING_FREQ_DIP, //
		HANDLE_FREQ_DIP, //
		FINISH_ACTIVATION
	}

	protected static record ActivationTimeState(SubState subState, Instant lastChange) {
	}

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		context.ess.setActivePowerEquals(ZERO_WATT_POWER);
		this.activationTimeState = new ActivationTimeState(SubState.INSIDE_TIME_FRAME, Instant.now(context.clock));
	}

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var nextSubState = this.getNextSubState(context);
		if (nextSubState != this.activationTimeState.subState) {
			this.activationTimeState = new ActivationTimeState(nextSubState, Instant.now(context.clock));
		}
		if (nextSubState == SubState.FINISH_ACTIVATION) {
			return State.SUPPORT_DURATION;
		}

		return State.ACTIVATION_TIME;
	}

	private SubState getNextSubState(Context context) throws OpenemsNamedException {
		return switch (this.activationTimeState.subState) {
		case INSIDE_TIME_FRAME ->
			this.isInsideTimeFrame(context) ? SubState.FINISH_ACTIVATION : SubState.HANDLE_WAITING_FREQ_DIP;
		case HANDLE_WAITING_FREQ_DIP -> {
			if (this.isFrequencyDipped(context)) {
				context.ess.setActivePowerEquals(context.dischargePower);
				var time = Instant.now(context.clock);
				this.clockActivationTime(context, time);
				this.dipDetectedStartTime = time;
				yield SubState.HANDLE_FREQ_DIP;
			}
			context.ess.setActivePowerEquals(ZERO_WATT_POWER);
			yield SubState.HANDLE_WAITING_FREQ_DIP;
		}
		case HANDLE_FREQ_DIP -> {
			context.ess.setActivePowerEquals(context.dischargePower);
			var activationExpirationTime = Duration.between(this.dipDetectedStartTime, Instant.now(context.clock))//
					.toMillis(); //
			if (activationExpirationTime >= context.activationRunTime.getValue()) {
				yield SubState.FINISH_ACTIVATION;
			}
			yield SubState.HANDLE_FREQ_DIP;
		}
		case FINISH_ACTIVATION -> SubState.FINISH_ACTIVATION;
		};
	}

	/**
	 * Clocks the activation time and sets it in the context.
	 *
	 * @param context the context.
	 * @param time    time in instant
	 */
	private void clockActivationTime(Context context, Instant time) {
		context.setCycleStart(time);
	}

	private boolean isFrequencyDipped(Context context) throws OpenemsException {
		var meterFrequency = context.meter.getFrequency();
		if (!meterFrequency.isDefined()) {
			throw new OpenemsException("meter has no frequency channel defined.");
		}
		return (meterFrequency.get() < context.freqLimit);
	}

	private boolean isInsideTimeFrame(Context context) {
		final var now = Instant.now(context.clock).getEpochSecond();
		final var startTimestamp = context.startTimestamp;
		final var duration = context.duration;
		return now >= startTimestamp + duration;
	}

	@Override
	protected String debugLog() {
		return State.ACTIVATION_TIME.asCamelCase() + "-"
				+ EnumUtils.nameAsCamelCase(this.activationTimeState.subState());
	}
}
