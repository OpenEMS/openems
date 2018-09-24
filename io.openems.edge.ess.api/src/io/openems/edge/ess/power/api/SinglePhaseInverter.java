package io.openems.edge.ess.power.api;

import io.openems.edge.ess.api.ManagedAsymmetricEss;

public class SinglePhaseInverter extends Inverter {

	public SinglePhaseInverter(ManagedAsymmetricEss ess, Phase phase) {
		super(ess, phase);
	}

}
