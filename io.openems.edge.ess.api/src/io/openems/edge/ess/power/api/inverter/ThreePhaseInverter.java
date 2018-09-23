package io.openems.edge.ess.power.api.inverter;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;

public class ThreePhaseInverter extends Inverter {

	public ThreePhaseInverter(ManagedSymmetricEss ess) {
		super(ess, Phase.ALL);
	}
	
}
