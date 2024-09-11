package io.openems.edge.energy.api;

public enum Version {
	/**
	 * Version 1.
	 * 
	 * <p>
	 * Well tested and production ready, but applies only to
	 * "Controller.Ess.Time-Of-Use-Tariff".
	 */
	V1_ESS_ONLY, //
	/**
	 * Version 1.
	 * 
	 * <p>
	 * Work-in-progress that uses new EnergySchedulable interface to provide real
	 * multi-objective optimization.
	 */
	V2_ENERGY_SCHEDULABLE
}
