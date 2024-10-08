package io.openems.edge.evcs.keba.kecontact.state;

import io.openems.edge.evcs.keba.kecontact.EvcsKebaKeContactImpl;

public class Inaktiv implements State {
	@Override
	public void handlePower(int power, EvcsKebaKeContactImpl context) {
		// System is inactive. No power application possible.

	}

	@Override
	public void switchPhase(EvcsKebaKeContactImpl context) {
		// No phase switch is allowed in inactive state.

	}
}
