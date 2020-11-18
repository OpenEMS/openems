package io.openems.edge.ess.power.api;

import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public abstract class Inverter {

	/*
	 * Factory
	 */
	public static Inverter[] of(boolean symmetricMode, ManagedSymmetricEss ess, EssType essType) {
		// Is this ESS a HybridEss?
		final boolean isHybridEss = (ess instanceof HybridEss);

		String essId = ess.id();
		if (symmetricMode) {
			// Symmetric Mode -> always return a symmetric ThreePhaseInverter
			switch (essType) {
			case SINGLE_PHASE:
			case ASYMMETRIC:
			case SYMMETRIC:
				return new Inverter[] { new ThreePhaseInverter(essId, isHybridEss) };

			case META:
				return new Inverter[0];
			}
		} else {
			// Asymmetric Mode
			switch (essType) {
			case SINGLE_PHASE:
				Phase phase = ((ManagedSinglePhaseEss) ess).getPhase().getPowerApiPhase();
				return new Inverter[] { //
						phase == Phase.L1 ? new SinglePhaseInverter(essId, Phase.L1, isHybridEss)
								: new DummyInverter(essId, Phase.L1, isHybridEss),
						phase == Phase.L2 ? new SinglePhaseInverter(essId, Phase.L2, isHybridEss)
								: new DummyInverter(essId, Phase.L2, isHybridEss),
						phase == Phase.L3 ? new SinglePhaseInverter(essId, Phase.L3, isHybridEss)
								: new DummyInverter(essId, Phase.L3, isHybridEss), //
				};

			case ASYMMETRIC:
				return new Inverter[] { //
						new SinglePhaseInverter(essId, Phase.L1, isHybridEss), //
						new SinglePhaseInverter(essId, Phase.L2, isHybridEss), //
						new SinglePhaseInverter(essId, Phase.L3, isHybridEss) //
				};

			case META:
				return new Inverter[0];

			case SYMMETRIC:
				return new Inverter[] { new ThreePhaseInverter(essId, isHybridEss) };
			}
		}
		// should never come here
		return new Inverter[0];
	}

	private final String essId;
	private final Phase phase;
	private final boolean isHybridEss;

	protected Inverter(String essId, Phase phase, boolean isHybridEss) {
		this.essId = essId;
		this.phase = phase;
		this.isHybridEss = isHybridEss;
	}

	public String getEssId() {
		return this.essId;
	}

	public Phase getPhase() {
		return this.phase;
	}

	/**
	 * Does this {@link Inverter} represent a {@link HybridEss}?.
	 * 
	 * @return true for {@link HybridEss}; false otherwise
	 */
	public boolean isHybridEss() {
		return isHybridEss;
	}

	/**
	 * Holds the weight of this Inverter in relation to other Inverters. Range
	 * [1-100]
	 */
	private int weight = 1;

	public Inverter setWeight(int weight) {
		if (weight > 100) {
			this.weight = 100;
		} else if (weight < 1) {
			this.weight = 1;
		} else {
			this.weight = weight;
		}
		return this;
	}

	public int getWeight() {
		return this.weight;
	}

	/**
	 * Holds the last set ActivePower
	 */
	private int lastActivePower = 0;

	public final void setLastActivePower(int activePower) {
		this.lastActivePower = activePower;
	}

	public int getLastActivePower() {
		return lastActivePower;
	}

	public String toString() {
		return this.essId + this.phase.getSymbol();
	}
}
