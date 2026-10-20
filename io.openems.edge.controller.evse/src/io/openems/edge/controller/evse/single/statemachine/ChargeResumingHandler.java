package io.openems.edge.controller.evse.single.statemachine;

import static io.openems.edge.controller.evse.single.Utils.CHARGE_THRESHOLD_IN_WATT;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.EvseSingleState;
import io.openems.edge.controller.evse.single.Mode;

public class ChargeResumingHandler extends StateHandler<EvseSingleState, Context> {

	@Override
	public EvseSingleState runAndGetNextState(Context context) {
		if (!context.actions.abilities().isReadyForCharging() || !context.actions.abilities().isEvConnected()) {
			return EvseSingleState.UNDEFINED;
		}

		if (context.actions.applySetPoint() == null) {
			return EvseSingleState.CHARGE_RESUMING;
		}

		if (context.actions.applySetPoint().value() == 0) {
			return context.mode == Mode.ZERO ? EvseSingleState.CHARGE_DISABLED : EvseSingleState.CHARGE_PAUSED;
		}

		context.applyActions();

		final var activePower = context.chargePoint.getActivePower();
		if (activePower.isDefined() && activePower.get() > CHARGE_THRESHOLD_IN_WATT) {
			return EvseSingleState.CHARGING;
		} else {
			return EvseSingleState.CHARGE_RESUMING;
		}
	}
}
