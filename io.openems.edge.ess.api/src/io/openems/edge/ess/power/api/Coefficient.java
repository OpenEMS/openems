package io.openems.edge.ess.power.api;

import io.openems.edge.ess.api.ManagedSymmetricEss;

public class Coefficient {

	private final ManagedSymmetricEss ess;
	private final Phase phase;
	private final Pwr pwr;
	private final double value;

	public Coefficient(ManagedSymmetricEss ess, Pwr pwr, double value) {
		this(ess, Phase.ALL, pwr, value);
	}

	public Coefficient(ManagedSymmetricEss ess, Phase phase, Pwr pwr, double value) {
		this.ess = ess;
		this.phase = phase;
		this.pwr = pwr;
		this.value = value;
	}

	@Override
	public String toString() {
		return "[" + ess.id() + "," + phase.name() + "," + pwr + "=" + value + "]";
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

	public double getValue() {
		return value;
	}
}
