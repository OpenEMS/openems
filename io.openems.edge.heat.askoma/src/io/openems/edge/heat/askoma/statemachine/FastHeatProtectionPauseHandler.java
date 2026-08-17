package io.openems.edge.heat.askoma.statemachine;

import static io.openems.edge.heat.askoma.statemachine.AskomaConstants.FAST_HEAT_PROTECTION_PAUSE_DURATION;
import static io.openems.edge.heat.askoma.statemachine.AskomaConstants.OFF_ACTIVE_POWER;

import java.time.Instant;

public class FastHeatProtectionPauseHandler extends AbstractFastHeatHandler {
	private Instant pauseStartedAt = null;

	@Override
	protected void onEntry(Context context) {
		context.setTargetActivePowerForHeatElement(OFF_ACTIVE_POWER);
	}

	@Override
	public StateMachine.State runAndGetNextState(Context context) {
		context.setFastHeatPowerNotApplied(false);

		if (this.pauseStartedAt == null) {
			this.pauseStartedAt = context.clock.instant();
		}

		if (!this.isFastHeatPauseExpired(context)) {
			context.setTargetActivePowerForHeatElement(OFF_ACTIVE_POWER);
			return StateMachine.State.FAST_HEAT_PROTECTION_PAUSE;
		}

		this.pauseStartedAt = null;
		this.applyMaxHeatPower(context);
		return StateMachine.State.FAST_HEAT;
	}

	@Override
	protected void onExit(Context context) {
		this.pauseStartedAt = null;
	}

	private boolean isFastHeatPauseExpired(Context context) {
		if (this.pauseStartedAt == null) {
			return false;
		}
		return !context.clock.instant().isBefore(this.pauseStartedAt.plus(FAST_HEAT_PROTECTION_PAUSE_DURATION));
	}
}
