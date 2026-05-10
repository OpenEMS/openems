package io.openems.edge.heat.askoma.statemachine;

import static io.openems.edge.heat.askoma.statemachine.AskomaConstants.OFF_ACTIVE_POWER;

public class FastHeatPauseHandler extends AbstractFastHeatHandler {

	@Override
	public StateMachine.State runAndGetNextState(Context context) {
		context.resetFastHeatPowerNotAppliedState();

		if (context.getFastHeatPauseStartedAt() == null) {
			context.setFastHeatPauseStartedAt(context.clock.instant());
		}

		if (!context.isFastHeatPauseExpired()) {
			context.setTargetActivePowerForHeatElement(OFF_ACTIVE_POWER);
			return StateMachine.State.FAST_HEAT_PAUSE;
		}

		context.setFastHeatPauseStartedAt(null);
		context.setFastHeatStartedAt(context.clock.instant());

		this.applyMaxHeatPower(context);
		return StateMachine.State.FAST_HEAT;
	}
}
