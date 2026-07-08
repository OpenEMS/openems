package io.openems.edge.ess.generic.symmetric.essfaultbehaviour;

import io.openems.edge.ess.generic.symmetric.statemachine.Context;

public class IgnoreBatteryInverterEssFaultBehaviour implements EssFaultBehaviour {

	@Override
	public boolean hasEssFault(Context context) {
		final var essFault = context.getParent().hasFaults();
		final var batteryFault = context.battery.hasFaults();
		context.getParent()._setEssFaultDueToBatteryFault(batteryFault);
		context.getParent()._setEssFaultDueToBatteryInverterFault(false);
		return essFault || batteryFault;
	}

	@Override
	public boolean isEssStarted(Context context) {
		return context.battery.isStarted();
	}

}
