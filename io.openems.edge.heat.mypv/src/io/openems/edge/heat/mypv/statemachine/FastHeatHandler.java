package io.openems.edge.heat.mypv.statemachine;

import static io.openems.edge.heat.mypv.statemachine.MyPvConstants.FAST_HEAT_DURATION;
import static io.openems.edge.heat.mypv.statemachine.MyPvConstants.OFF_ACTIVE_POWER;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class FastHeatHandler extends AbstractFastHeatHandler {
	private static final Duration POWER_NOT_APPLIED_DELAY = Duration.ofMinutes(5);
	private Instant fastHeatPowerNotAppliedSince = null;
	private Instant fastHeatStartedAt;

	@Override
	protected void onEntry(Context context) {
		this.fastHeatStartedAt = context.clock.instant();
		this.resetFastHeatPowerNotAppliedState(context);
	}

	@Override
	public StateMachine.State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (this.isFastHeatExpired(context.clock)) {
			context.setTargetActivePowerForHeatElement(OFF_ACTIVE_POWER);
			return StateMachine.State.FAST_HEAT_PROTECTION_PAUSE;
		}

		this.applyMaxHeatPower(context);
		this.updateFastHeatPowerResponse(context);
		return StateMachine.State.FAST_HEAT;
	}

	@Override
	protected void onExit(Context context) {
		this.fastHeatStartedAt = null;
		this.resetFastHeatPowerNotAppliedState(context);
	}

	private void updateFastHeatPowerResponse(Context context) {
		if (this.isTargetActivePowerApplied(context)) {
			this.resetFastHeatPowerNotAppliedState(context);
			return;
		}

		if (this.fastHeatPowerNotAppliedSince == null) {
			this.fastHeatPowerNotAppliedSince = context.clock.instant();
			context.setFastHeatPowerNotApplied(false);
			return;
		}

		var isDelayElapsed = !context.clock.instant()
				.isBefore(this.fastHeatPowerNotAppliedSince.plus(POWER_NOT_APPLIED_DELAY));
		context.setFastHeatPowerNotApplied(isDelayElapsed);
	}

	private void resetFastHeatPowerNotAppliedState(Context context) {
		this.fastHeatPowerNotAppliedSince = null;
		context.setFastHeatPowerNotApplied(false);
	}

	private boolean isTargetActivePowerApplied(Context context) {
		var targetActivePower = context.getRequestedTargetActivePower();
		if (targetActivePower == null || targetActivePower <= 0) {
			return false;
		}

		var activePower = context.getActivePower();
		return activePower > 0;
	}

	private boolean isFastHeatExpired(Clock clock) {
		var startedAt = this.fastHeatStartedAt;
		if (startedAt == null) {
			return false;
		}
		return !clock.instant().isBefore(startedAt.plus(FAST_HEAT_DURATION));
	}
}
