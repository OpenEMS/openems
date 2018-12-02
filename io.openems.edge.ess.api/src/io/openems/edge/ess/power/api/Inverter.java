package io.openems.edge.ess.power.api;

public abstract class Inverter {

	/*
	 * Factory
	 */
	public static Inverter[] of(String essId, EssType essType, boolean symmetricMode) {
		switch (essType) {
		case ASYMMETRIC:
			if (symmetricMode) {
				return new Inverter[] { new ThreePhaseInverter(essId) };
			} else {
				return new Inverter[] { //
						new SinglePhaseInverter(essId, Phase.L1), //
						new SinglePhaseInverter(essId, Phase.L2), //
						new SinglePhaseInverter(essId, Phase.L3) //
				};
			}

		case META:
			return new Inverter[0];

		case SYMMETRIC:
			return new Inverter[] { new ThreePhaseInverter(essId) };
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
	private int weight = 0;

	public void setWeight(int weight) {
		if (weight > 100) {
			this.weight = 100;
		} else if (weight < 0) {
			this.weight = 0;
		} else {
			this.weight = weight;
		}
	}

	public int getWeight() {
		return weight;
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
