package io.openems.edge.heat.askoma.statemachine;

import static io.openems.edge.heat.askoma.statemachine.AskomaConstants.OFF_ACTIVE_POWER;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastHeatHandler extends AbstractFastHeatHandler {
	private static final Logger log = LoggerFactory.getLogger(FastHeatHandler.class);
	private static final Duration POWER_NOT_APPLIED_DELAY = Duration.ofMinutes(5);

	@Override
	public StateMachine.State runAndGetNextState(Context context) {
		if (context.getFastHeatStartedAt() == null) {
			context.setFastHeatStartedAt(context.clock.instant());
			context.resetFastHeatPowerNotAppliedState();
		}

		context.logInfo(log, "handle Fast Heat");

		if (context.isFastHeatExpired()) {
			context.setTargetActivePowerForHeatElement(OFF_ACTIVE_POWER);
			context.resetFastHeatPowerNotAppliedState();
			context.setFastHeatStartedAt(null);
			return StateMachine.State.FAST_HEAT_PAUSE;
		}

		this.applyMaxHeatPower(context);
		this.updateFastHeatPowerResponse(context);
		return StateMachine.State.FAST_HEAT;
	}

	private void updateFastHeatPowerResponse(Context context) {
		if (this.isTargetGridActivePowerApplied(context)) {
			context.resetFastHeatPowerNotAppliedState();
			return;
		}

		var powerNotAppliedSince = context.getFastHeatPowerNotAppliedSince();
		if (powerNotAppliedSince == null) {
			context.setFastHeatPowerNotAppliedSince(context.clock.instant());
			context.setFastHeatPowerNotApplied(false);
			return;
		}

		var isDelayElapsed = !context.clock.instant().isBefore(powerNotAppliedSince.plus(POWER_NOT_APPLIED_DELAY));
		context.setFastHeatPowerNotApplied(isDelayElapsed);
	}

	private boolean isTargetGridActivePowerApplied(Context context) {
		var targetGridActivePower = context.getRequestedTargetGridActivePower();
		if (targetGridActivePower == null || targetGridActivePower >= 0) {
			return false;
		}

		var activePower = context.getActivePower();
		return activePower != null && activePower > 0;
	}

}
