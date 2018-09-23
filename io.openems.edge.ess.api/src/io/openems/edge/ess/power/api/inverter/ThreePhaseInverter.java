package io.openems.edge.ess.power.api.inverter;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;

public class ThreePhaseInverter extends Inverter {

	public ThreePhaseInverter(ManagedSymmetricEss ess) {
		super(ess, Phase.ALL);
	}

	@Override
	public void updateMinMax(int minP, int maxP) {
//		int maxApparent = this.getEss().getMaxApparentPower().value().orElse(0);
//		int allowedDischarge = this.getEss().getAllowedDischarge().value().orElse(0);
//		int allowedCharge = this.getEss().getAllowedCharge().value().orElse(0);
//		this.maxP = Math.min(maxApparent, Math.min(allowedDischarge, maxP));
//		this.minP = Math.max(maxApparent * -1, Math.max(allowedCharge, minP));
	}
}
