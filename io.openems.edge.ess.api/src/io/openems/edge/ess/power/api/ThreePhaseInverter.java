package io.openems.edge.ess.power.api;

import io.openems.edge.ess.api.ManagedSymmetricEss;

public class ThreePhaseInverter extends Inverter {

	public ThreePhaseInverter(ManagedSymmetricEss ess) {
		super(ess, Phase.ALL);
	}
	
}
