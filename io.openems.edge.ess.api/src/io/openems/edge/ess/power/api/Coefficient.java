package io.openems.edge.ess.power.api;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public class Coefficient {

	private final ManagedSymmetricEss ess;
	private final Phase phase;
	private final Pwr pwr;
	private final int value;

	public Coefficient(ManagedSymmetricEss ess, Pwr pwr, int value) {
		this(ess, Phase.ALL, pwr, value);
	}

	public Coefficient(ManagedSymmetricEss ess, Phase phase, Pwr pwr, int value) {
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

	public int getValue() {
		return value;
	}
}
