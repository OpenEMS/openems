package io.openems.edge.ess.core.power.inverter;

import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.power.api.Phase;

public class SinglePhaseInverter extends Inverter {

	private final Phase phase;

	public SinglePhaseInverter(ManagedAsymmetricEss ess, Phase phase) {
		super(ess);
		this.phase = phase;
	}

	public Phase getPhase() {
		return phase;
	}

	public String getId() {
		return this.getEss().id() + phase.name();
	}

	@Override
	public void updateMinMax(int minP, int maxP) {
		int maxApparent = this.getEss().getMaxApparentPower().value().orElse(0) / 3;
		int allowedDischarge = this.getEss().getAllowedDischarge().value().orElse(0) / 3;
		int allowedCharge = this.getEss().getAllowedCharge().value().orElse(0) / 3;
		this.maxP = Math.min(maxApparent, Math.min(allowedDischarge, maxP));
		this.minP = Math.max(maxApparent * -1, Math.max(allowedCharge, minP));
	}
}
