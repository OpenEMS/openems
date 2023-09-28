package io.openems.edge.battery.fenecon.f2b.bmw.statemachine;

import io.openems.edge.battery.fenecon.f2b.bmw.enums.HvContactorStatus;
import io.openems.edge.battery.fenecon.f2b.bmw.statemachine.StateMachine.State;
import io.openems.edge.battery.fenecon.f2b.common.enums.F2bState;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var battery = context.getParent();

		if (battery.getF2bState().isUndefined() || battery.getF2bState() == F2bState.UNDEFINED) {
			return State.UNDEFINED;
		}

		var hvContactorsStatus = battery.getHvContactorStatus();
		if (hvContactorsStatus.isDefined() && hvContactorsStatus.asEnum() == HvContactorStatus.CONTACTORS_CLOSED) {
			return State.RUNNING;
		}

		if (battery.getF2bTerminal30c().isDefined()//
				&& !battery.getF2bTerminal30c().get()//
				&& !context.isF2bCanCommunicationOn()) {
			return State.STOPPED;
		}

		return State.GO_STOPPED;
	}
}
