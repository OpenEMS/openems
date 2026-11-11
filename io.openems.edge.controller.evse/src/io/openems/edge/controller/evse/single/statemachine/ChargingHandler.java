package io.openems.edge.controller.evse.single.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.EvseSingleState;
import io.openems.edge.controller.evse.single.Mode;

public class ChargingHandler extends StateHandler<EvseSingleState, Context> {

	@Override
	public EvseSingleState runAndGetNextState(Context context) {
		if (!context.actions.abilities().isReadyForCharging() || !context.actions.abilities().isEvConnected()) {
			return EvseSingleState.UNDEFINED;
		}

		final var history = context.history;
		if (history.getAppearsToBeFullyCharged()) {
			// -> Charging finished by EV
			context.applyMinSetPointActions();
			return EvseSingleState.FINISHED_EV_STOP;
		}

		if (context.reachedSessionLimit) {
			// Session Energy Limit was reached
			return EvseSingleState.FINISHED_ENERGY_SESSION_LIMIT;
		}

		if (context.actions.phaseSwitch() != null) {
			context.applyActions();

			return switch (context.actions.phaseSwitch().direction()) {
			case TO_SINGLE_PHASE -> EvseSingleState.PHASE_SWITCH_TO_SINGLE_PHASE;
			case TO_THREE_PHASE -> EvseSingleState.PHASE_SWITCH_TO_THREE_PHASE;
			};
		}

		if (context.actions.applySetPoint() != null && context.actions.applySetPoint().value() == 0) {
			if (context.mode == Mode.SURPLUS) {
				context.history.setLastChargeStateChangeTriggeredBySurplus(context.clock.instant());
			}
			return context.mode == Mode.ZERO ? EvseSingleState.CHARGE_DISABLED : EvseSingleState.CHARGE_PAUSED;
		}

		// Apply Actions directly
		context.applyActions();

		return EvseSingleState.CHARGING;
	}
}
