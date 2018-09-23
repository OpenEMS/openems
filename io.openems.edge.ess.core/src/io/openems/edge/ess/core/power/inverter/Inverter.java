package io.openems.edge.ess.core.power.inverter;

import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;

public abstract class Inverter {

	private final ManagedSymmetricEss ess;

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

	/**
	 * Holds the last set P
	 * 
	 * @param activePower
	 * @param reactivePower
	 */
	protected int lastP = 0;

	/**
	 * Holds the weight of this ESS in relation to other ESSs
	 */
	public int weight = 0;

	/**
	 * Holds the target P that the Solver is trying to approach (e.g. to optimize
	 * efficiency)
	 */
	protected float targetP = 0;

	/**
	 * Holds the learning rate towards the target
	 */
	protected float learningRate = 0;

	/**
	 * Holds a temporary value for nextP as a float
	 */
	protected float floatNextP = 0;

	/**
	 * Holds the value that should be set as next P
	 */
	public int nextP = 0;

	/**
	 * Holds the maximum allowed discharge P
	 */
	public int maxP = 0;

	/**
	 * Holds the minimum allowed charge P
	 */
	public int minP = 0;

	protected Inverter(ManagedSymmetricEss ess) {
		this.ess = ess;
	}

	public final void applyPower() {
		this.lastP = this.nextP;
		// this.lastQ = this.nextQ;
	}

	public ManagedSymmetricEss getEss() {
		return this.ess;
	}

	public String getId() {
		return this.getEss().id();
	}

	public abstract void updateMinMax(int minP, int maxP);
}
