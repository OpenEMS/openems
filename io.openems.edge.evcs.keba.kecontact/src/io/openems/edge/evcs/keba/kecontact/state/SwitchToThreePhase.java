package io.openems.edge.evcs.keba.kecontact.state;

import io.openems.edge.evcs.keba.kecontact.EvcsKebaKeContactImpl;
import io.openems.edge.evcs.api.Phases;

public class SwitchToThreePhase implements State {
	@Override
	public void switchPhase(EvcsKebaKeContactImpl context) {
		context.getPhaseSwitchHandler().handlePhaseSwitch(Phases.THREE_PHASE);
		context.getPhaseSwitchHandler().setState(context.getRunningThreePhaseState());
	}

	@Override
	public void handlePower(int power, EvcsKebaKeContactImpl context) {
	}
}
