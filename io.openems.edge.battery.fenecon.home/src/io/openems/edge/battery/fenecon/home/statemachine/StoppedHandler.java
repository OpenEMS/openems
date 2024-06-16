package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.edge.battery.fenecon.home.BatteryFeneconHomeImpl;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var battery = context.getParent();

		final int currentMinVoltage;
		if (battery.getMinCellVoltage().isDefined()) {
			currentMinVoltage = battery.getMinCellVoltage().get();
		} else {
			currentMinVoltage = battery.getMinCellVoltageChannel().getPastValues().lastEntry().getValue()
					.orElse(Integer.MAX_VALUE);
		}

		if (currentMinVoltage < BatteryFeneconHomeImpl.DEFAULT_CRITICAL_MIN_VOLTAGE) {
			battery._setLowMinVoltageFaultBatteryStopped(true);
		} else {
			battery._setLowMinVoltageFaultBatteryStopped(false);
		}

		// Mark as started
		battery._setStartStop(StartStop.STOP);
		return State.STOPPED;
	}

}
