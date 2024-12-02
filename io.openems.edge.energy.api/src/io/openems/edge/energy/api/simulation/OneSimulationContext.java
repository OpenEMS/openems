package io.openems.edge.energy.api.simulation;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.lang.Math.max;

import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;

/**
 * Holds the simulation context that is used for one simulation of a full
 * schedule with multiple periods.
 * 
 * <p>
 * This record is usually created multiple times per second.
 */
public class OneSimulationContext {

	public static class Ess {
		/** ESS Currently Available Energy (SoC in [Wh]). */
		private int initialEnergy;

		protected static Ess from(GlobalSimulationsContext.Ess ess) {
			return new Ess(ess.currentEnergy());
		}

		private Ess(int initialEnergy) {
			this.initialEnergy = initialEnergy;
		}

		/**
		 * Calculates the initial SoC-Energy of the next Period.
		 * 
		 * @param ess the ess charge/discharge energy of the current Period
		 */
		public synchronized void calculateInitialEnergy(int ess) {
			this.initialEnergy = max(0, this.initialEnergy - ess); // always at least '0'
		}

		/**
		 * The initial SoC-Energy of the Period.
		 * 
		 * @return the value
		 */
		public int getInitialEnergy() {
			return this.initialEnergy;
		}
	}

	public static class Evcs {
		// Energy Session [Wh]
		private int initialEnergySession;

		protected static Evcs from(GlobalSimulationsContext.Evcs evcs) {
			return new Evcs(evcs.energySession());
		}

		private Evcs(int initialEnergySession) {
			this.initialEnergySession = initialEnergySession;
		}

		/**
		 * Calculates the initial EnergySession of the next Period.
		 * 
		 * @param evcs the evcs charge energy of the current Period
		 */
		public synchronized void calculateInitialEnergySession(int evcs) {
			this.initialEnergySession = this.initialEnergySession + max(0, evcs);
		}

		/**
		 * The initial EnergySession of the Period.
		 * 
		 * @return the value
		 */
		public int getInitialEnergySession() {
			return this.initialEnergySession;
		}
	}

	/**
	 * Builds a {@link OneSimulationContext}.
	 * 
	 * @param asc the {@link GlobalSimulationsContext}
	 * @return the {@link OneSimulationContext}
	 */
	public static OneSimulationContext from(GlobalSimulationsContext asc) {
		return new OneSimulationContext(asc, Ess.from(asc.ess()),
				asc.evcss().entrySet().stream().collect(ImmutableMap.toImmutableMap(//
						Entry::getKey, //
						e -> Evcs.from(e.getValue()))));
	}

	public final GlobalSimulationsContext global;
	public final Ess ess;
	public final ImmutableMap<String, Evcs> evcss;

	private OneSimulationContext(GlobalSimulationsContext gsc, Ess ess, ImmutableMap<String, Evcs> evcss) {
		this.global = gsc;
		this.ess = ess;
		this.evcss = evcss;
	}

	@Override
	public String toString() {
		return toStringHelper(this) //
				.add("ess", this.ess) //
				.addValue(this.global) //
				.toString();
	}
}
