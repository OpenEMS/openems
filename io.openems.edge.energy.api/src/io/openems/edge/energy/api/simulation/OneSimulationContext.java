package io.openems.edge.energy.api.simulation;

import static java.lang.Math.max;

/**
 * Holds the simulation context that is used for one simulation of a full
 * schedule with multiple periods.
 * 
 * <p>
 * This record is usually created multiple times per second.
 */
public class OneSimulationContext {

	/**
	 * Builds a {@link OneSimulationContext}.
	 * 
	 * @param asc the {@link GlobalSimulationsContext}
	 * @return the {@link OneSimulationContext}
	 */
	public static OneSimulationContext from(GlobalSimulationsContext asc) {
		return new OneSimulationContext(asc, asc.ess().currentEnergy());
	}

	public final GlobalSimulationsContext global;

	private int essInitialEnergy;

	private OneSimulationContext(GlobalSimulationsContext asc, int essInitialEnergy) {
		this.global = asc;
		this.essInitialEnergy = essInitialEnergy;
	}

	/**
	 * Calculates the initial SoC-Energy of the next Period.
	 * 
	 * @param ess the ess charge/discharge energy of the current Period
	 */
	public synchronized void calculateEssInitial(int ess) {
		this.essInitialEnergy = max(0, this.essInitialEnergy - ess); // always at least '0'
	}

	/**
	 * The initial SoC-Energy of the Period.
	 * 
	 * @return the value
	 */
	public int getEssInitial() {
		return this.essInitialEnergy;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append("OneSimulationContext [") //
				.append("essInitialEnergy=").append(this.essInitialEnergy).append(", ") //
				.append("global=").append(this.global) //
				.append("]") //
				.toString();
	}
}
