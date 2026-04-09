package io.openems.edge.controller.ess.fixstateofcharge.statemachine;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.controller.ess.fixstateofcharge.api.FixStateOfCharge.ChannelId.EXPECTED_START_EPOCH_SECONDS;

import java.time.ZonedDateTime;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fixstateofcharge.api.AbstractFixStateOfCharge;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.StateMachine.State;

public class NotStartedHandler extends StateHandler<State, Context> {

	private static final long THRESHOLD_SECONDS = 30; // Only update if changed by > 30 seconds
	private Long lastSetExpectedStartEpochSeconds = null;

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		// Immediate start: no target time configured or target time already passed
		if (this.shouldStartImmediately(context)) {
			this.clearExpectedStartEpochSeconds(context);
			return this.transitionToNextState(context);
		}

		var calculatedStartTime = this.calculateStartTime(context);
		var currentTime = ZonedDateTime.now(context.clock);
		var isStillWaiting = calculatedStartTime.isAfter(currentTime);

		if (isStillWaiting) {
			this.updateExpectedStartEpochSecondsIfChanged(context, calculatedStartTime);
			return State.NOT_STARTED;
		}

		// Start time reached - begin charging/discharging
		this.clearExpectedStartEpochSeconds(context);
		return this.transitionToNextState(context);
	}

	private boolean shouldStartImmediately(Context context) {
		return !context.considerTargetTime() || context.passedTargetTime();
	}

	private ZonedDateTime calculateStartTime(Context context) throws InvalidValueException {
		var capacity = context.getEssCapacityForEstimationWh();
		var bufferSeconds = context.config.getTargetTimeBuffer() * 60;

		long requiredSeconds;
		if (context.getParent().isReferenceCycleEnabled()) {
			requiredSeconds = ReferenceCycleUtils.calculateRequiredTimeWithReferenceCycle(context);
		} else {
			var power = context.getTimeEstimationPowerW(capacity);
			requiredSeconds = AbstractFixStateOfCharge.calculateRequiredTime(context.soc, context.targetSoc, capacity,
					power, context.clock);
		}

		return context.getTargetTime().minusSeconds(requiredSeconds + bufferSeconds);
	}

	private State transitionToNextState(Context context) {
		if (context.getParent().isReferenceCycleEnabled()) {
			return State.REFERENCE_CYCLE;
		}
		return Context.getSocState(context.soc, context.targetSoc);
	}

	private void updateExpectedStartEpochSecondsIfChanged(Context context, ZonedDateTime calculatedStartTime) {
		var newEpochSeconds = calculatedStartTime.toEpochSecond();

		// If not yet set, always set the first time
		if (this.lastSetExpectedStartEpochSeconds == null) {
			setValue(context.getParent(), EXPECTED_START_EPOCH_SECONDS,	newEpochSeconds);
			this.lastSetExpectedStartEpochSeconds = newEpochSeconds;
			return;
		}

		// Only update if the change is significant (> THRESHOLD_SECONDS)
		var difference = Math.abs(newEpochSeconds - this.lastSetExpectedStartEpochSeconds);
		if (difference > THRESHOLD_SECONDS) {
			setValue(context.getParent(), EXPECTED_START_EPOCH_SECONDS, newEpochSeconds);
			this.lastSetExpectedStartEpochSeconds = newEpochSeconds;
		}
	}

	private void clearExpectedStartEpochSeconds(Context context) {
		setValue(context.getParent(), EXPECTED_START_EPOCH_SECONDS, null);
		this.lastSetExpectedStartEpochSeconds = null;
	}
}
