package io.openems.edge.ess.power.api;

import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public abstract class Inverter {

	/**
	 * Factory for an {@link Inverter}.
	 * 
	 * @param symmetricMode is Symmetric-Mode?
	 * @param ess           the {@link ManagedSymmetricEss}
	 * @param essType       the {@link EssType}
	 * @return the {@link Inverter}
	 */
	public static Inverter[] of(boolean symmetricMode, ManagedSymmetricEss ess, EssType essType) {
		var essId = ess.id();
		if (symmetricMode) {
			// Symmetric Mode -> always return a symmetric ThreePhaseInverter
			switch (essType) {
			case SINGLE_PHASE:
			case ASYMMETRIC:
			case SYMMETRIC:
				return new Inverter[] { new ThreePhaseInverter(essId) };

			case META:
				return new Inverter[0];
			}
		} else {
			// Asymmetric Mode
			switch (essType) {
			case SINGLE_PHASE:
				var phase = ((ManagedSinglePhaseEss) ess).getPhase().getPowerApiPhase();
				return new Inverter[] { //
						phase == Phase.L1 ? new SinglePhaseInverter(essId, Phase.L1)
								: new DummyInverter(essId, Phase.L1),
						phase == Phase.L2 ? new SinglePhaseInverter(essId, Phase.L2)
								: new DummyInverter(essId, Phase.L2),
						phase == Phase.L3 ? new SinglePhaseInverter(essId, Phase.L3)
								: new DummyInverter(essId, Phase.L3), //
				};

			case ASYMMETRIC:
				return new Inverter[] { //
						new SinglePhaseInverter(essId, Phase.L1), //
						new SinglePhaseInverter(essId, Phase.L2), //
						new SinglePhaseInverter(essId, Phase.L3) //
				};

			case META:
				return new Inverter[0];

			case SYMMETRIC:
				return new Inverter[] { new ThreePhaseInverter(essId) };
			}
		}
		// should never come here
		return new Inverter[0];
	}

	private final String essId;
	private final Phase phase;

	protected Inverter(String essId, Phase phase) {
		this.essId = essId;
		this.phase = phase;
	}

	public String getEssId() {
		return this.essId;
	}

	public Phase getPhase() {
		return this.phase;
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
	 * Holds the last set ActivePower.
	 */
	private int lastActivePower = 0;

	public final void setLastActivePower(int activePower) {
		this.lastActivePower = activePower;
	}

	public int getLastActivePower() {
		return this.lastActivePower;
	}

	@Override
	public String toString() {
		return this.essId + this.phase.getSymbol();
	}
}
