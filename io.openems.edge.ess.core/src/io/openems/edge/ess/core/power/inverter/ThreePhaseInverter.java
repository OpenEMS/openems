package io.openems.edge.ess.core.power.inverter;

import io.openems.edge.ess.api.ManagedSymmetricEss;

public class ThreePhaseInverter extends Inverter {

	public ThreePhaseInverter(ManagedSymmetricEss ess) {
		super(ess);
	}

	public String getId() {
		return this.getEss().id();
	}

	@Override
	public void updateMinMax(int minP, int maxP) {
		int maxApparent = this.getEss().getMaxApparentPower().value().orElse(0);
		int allowedDischarge = this.getEss().getAllowedDischarge().value().orElse(0);
		int allowedCharge = this.getEss().getAllowedCharge().value().orElse(0);
		this.maxP = Math.min(maxApparent, Math.min(allowedDischarge, maxP));
		this.minP = Math.max(maxApparent * -1, Math.max(allowedCharge, minP));
	}
}
