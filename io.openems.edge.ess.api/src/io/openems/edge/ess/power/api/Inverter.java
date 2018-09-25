package io.openems.edge.ess.power.api;

import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public abstract class Inverter {

	/*
	 * Factory
	 */
	public static Inverter[] of(ManagedSymmetricEss ess, boolean symmetricMode) {
		if (ess instanceof ManagedAsymmetricEss && !symmetricMode) {
			ManagedAsymmetricEss e = (ManagedAsymmetricEss) ess;
			return new Inverter[] { //
					new SinglePhaseInverter(e, Phase.L1), //
					new SinglePhaseInverter(e, Phase.L2), //
					new SinglePhaseInverter(e, Phase.L3) //
			};
		} else {
			return new Inverter[] { new ThreePhaseInverter(ess) };
		}
	}

	private final ManagedSymmetricEss ess;
	private final Phase phase;

	/**
	 * Holds the weight of this Inverter in relation to other Inverters. Range
	 * [1-100]
	 */
	// TODO setWeight() that limits range to 1-100
	public int weight = 0;

	/**
	 * Holds the last set P
	 */
	public int lastP = 0;

	protected Inverter(ManagedSymmetricEss ess, Phase phase) {
		this.ess = ess;
		this.phase = phase;
	}

	public final void storeLastPower(int p, int q) {
		this.lastP = p;
		// this.lastQ = this.nextQ;
	}

	public ManagedSymmetricEss getEss() {
		return this.ess;
	}

	public Phase getPhase() {
		return phase;
	}

	public String toString() {
		return this.getEss().id() + phase.getSymbol();
	}
}
