package io.openems.edge.ess.power.api;

import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.common.type.Phase.SinglePhase;
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
			return switch (essType) {
			case SINGLE_PHASE, ASYMMETRIC, SYMMETRIC //
				-> new Inverter[] { new ThreePhaseInverter(essId) };
			case META //
				-> new Inverter[0];
			};
		} else {
			// Asymmetric Mode
			return switch (essType) {
			case SINGLE_PHASE -> {
				var phase = ((ManagedSinglePhaseEss) ess).getPhase();
				yield new Inverter[] { //
						phase == SinglePhase.L1 //
								? new SinglePhaseInverter(essId, phase) //
								: new DummyInverter(essId, phase.toSingleOrAllPhase), //
						phase == SinglePhase.L2 //
								? new SinglePhaseInverter(essId, phase) //
								: new DummyInverter(essId, phase.toSingleOrAllPhase), //
						phase == SinglePhase.L3 //
								? new SinglePhaseInverter(essId, phase) //
								: new DummyInverter(essId, phase.toSingleOrAllPhase) //
				};
			}
			case ASYMMETRIC -> new Inverter[] { //
					new SinglePhaseInverter(essId, SinglePhase.L1), //
					new SinglePhaseInverter(essId, SinglePhase.L2), //
					new SinglePhaseInverter(essId, SinglePhase.L3) //
				};
			case META //
				-> new Inverter[0];
			case SYMMETRIC //
				-> new Inverter[] { new ThreePhaseInverter(essId) };
			};
		}
	}

	private final String essId;
	private final SingleOrAllPhase phase;

	protected Inverter(String essId, SingleOrAllPhase phase) {
		this.essId = essId;
		this.phase = phase;
	}

	public String getEssId() {
		return this.essId;
	}

	public SingleOrAllPhase getPhase() {
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
		return this.essId + this.phase.symbol;
	}
}
