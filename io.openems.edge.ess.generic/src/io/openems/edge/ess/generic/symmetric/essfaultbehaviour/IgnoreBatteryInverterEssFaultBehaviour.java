package io.openems.edge.ess.generic.symmetric.essfaultbehaviour;

import io.openems.edge.ess.generic.symmetric.statemachine.Context;

public class IgnoreBatteryInverterEssFaultBehaviour implements EssFaultBehaviour {

	@Override
	public boolean hasEssFault(Context context) {
		return context.getParent().hasFaults() || context.battery.hasFaults();
	}

	@Override
	public boolean isEssStarted(Context context) {
		return context.battery.isStarted();
	}

}
