package io.openems.edge.evcs.keba.kecontact.state;

import io.openems.edge.evcs.keba.kecontact.EvcsKebaKeContactImpl;
import io.openems.edge.evcs.api.Phases;

public class RunningOnePhase implements State {
	@Override
	public void handlePower(int power, EvcsKebaKeContactImpl context) {
		if (power > context.getMaxPowerOnePhase()) {
			context.getPhaseSwitchHandler().setState(context.getSwitchToThreePhaseState());
			context.getPhaseSwitchHandler().switchPhase();
		} else {
			int current = context.calculateCurrent(power, 1);
			context.send("currtime " + current + " 1");
			context.send("Running on one phase with sufficient power.");
		}
	}

	@Override
	public void switchPhase(EvcsKebaKeContactImpl context) {
		context.getPhaseSwitchHandler().handlePhaseSwitch(Phases.THREE_PHASE);
		context.getPhaseSwitchHandler().setState(context.getRunningThreePhaseState());
	}
}
