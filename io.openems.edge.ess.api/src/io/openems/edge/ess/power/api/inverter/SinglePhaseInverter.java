package io.openems.edge.ess.power.api.inverter;

import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.power.api.Phase;

public class SinglePhaseInverter extends Inverter {

	public SinglePhaseInverter(ManagedAsymmetricEss ess, Phase phase) {
		super(ess, phase);
	}

	@Override
	public void updateMinMax(int minP, int maxP) {
//		int maxApparent = this.getEss().getMaxApparentPower().value().orElse(0) / 3;
//		int allowedDischarge = this.getEss().getAllowedDischarge().value().orElse(0) / 3;
//		int allowedCharge = this.getEss().getAllowedCharge().value().orElse(0) / 3;
//		this.maxP = Math.min(maxApparent, Math.min(allowedDischarge, maxP));
//		this.minP = Math.max(maxApparent * -1, Math.max(allowedCharge, minP));
	}
}
