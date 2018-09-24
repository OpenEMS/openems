package io.openems.edge.ess.power.api;

import io.openems.edge.ess.api.ManagedSymmetricEss;

public class Coefficient {

	protected final int index;
	protected final ManagedSymmetricEss ess;
	protected final Phase phase;
	protected final Pwr pwr;

	public Coefficient(int index, ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
		this.index = index;
		this.ess = ess;
		this.phase = phase;
		this.pwr = pwr;
	}

	@Override
	public String toString() {
		return this.ess.id() + this.pwr.getSymbol() + this.phase.getSymbol();
	}

	public int getIndex() {
		return index;
	}

	public ManagedSymmetricEss getEss() {
		return ess;
	}

	public Phase getPhase() {
		return phase;
	}

	public Pwr getPwr() {
		return pwr;
	}
}
