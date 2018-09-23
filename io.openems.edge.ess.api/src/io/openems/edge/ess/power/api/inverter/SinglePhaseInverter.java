package io.openems.edge.ess.power.api.inverter;

import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.power.api.Phase;

public class SinglePhaseInverter extends Inverter {

	public SinglePhaseInverter(ManagedAsymmetricEss ess, Phase phase) {
		super(ess, phase);
	}

}
