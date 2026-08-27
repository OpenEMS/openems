package io.openems.edge.heat.askoma.statemachine;

import static io.openems.edge.heat.askoma.statemachine.AskomaConstants.FAST_HEAT_DURATION;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

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
	public StateMachine.State runAndGetNextState(Context context) {
		if (this.isFastHeatExpired(context.clock)) {
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
		if (this.isTargetGridActivePowerApplied(context)) {
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

	private boolean isTargetGridActivePowerApplied(Context context) {
		var targetGridActivePower = context.getRequestedTargetGridActivePower();
		if (targetGridActivePower == null || targetGridActivePower >= 0) {
			return false;
		}

		var activePower = context.getActivePower();
		return activePower != null && activePower > 0;
	}

	private boolean isFastHeatExpired(Clock clock) {
		var startedAt = this.fastHeatStartedAt;
		if (startedAt == null) {
			return false;
		}
		return !clock.instant().isBefore(startedAt.plus(FAST_HEAT_DURATION));
	}

}
