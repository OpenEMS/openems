package io.openems.edge.controller.evse.single.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.EvseSingleState;
import io.openems.edge.controller.evse.single.Mode;

public class ChargePausedHandler extends StateHandler<EvseSingleState, Context> {

	@Override
	public EvseSingleState runAndGetNextState(Context context) {
		if (!context.actions.abilities().isReadyForCharging() || !context.actions.abilities().isEvConnected()) {
			return EvseSingleState.UNDEFINED;
		}

		if (context.mode == Mode.ZERO) {
			return EvseSingleState.CHARGE_DISABLED;
		}

		if (context.actions.applySetPoint() != null && context.actions.applySetPoint().value() > 0) {
			if (context.mode == Mode.SURPLUS) {
				context.history.setLastChargeStateChangeTriggeredBySurplus(context.clock.instant());
			}
			return EvseSingleState.CHARGE_RESUMING;
		}

		if (context.actions.phaseSwitch() != null) {
			context.applyActions();

			return switch (context.actions.phaseSwitch().direction()) {
			case TO_SINGLE_PHASE -> EvseSingleState.PHASE_SWITCH_TO_SINGLE_PHASE;
			case TO_THREE_PHASE -> EvseSingleState.PHASE_SWITCH_TO_THREE_PHASE;
			};
		}

		context.applyAdjustedActions(a -> a.setCorrectApplySetPointByWatt(0));
		return EvseSingleState.CHARGE_PAUSED;
	}
}
