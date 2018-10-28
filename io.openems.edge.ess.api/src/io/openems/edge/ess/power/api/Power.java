package io.openems.edge.ess.power.api;

import io.openems.edge.ess.api.ManagedSymmetricEss;

public interface Power {

	public static Constraint[] NO_CONSTRAINTS = new Constraint[] {};

	/**
	 * Adds a Constraint.
	 * 
	 * @param type
	 * @param constraint
	 */
	public Constraint addConstraint(Constraint constraint);

	/**
	 * Adds a Constraint if the problem is still solvable afterwards.
	 * 
	 * @param type
	 * @param constraint
	 * @throws PowerException
	 */
	public Constraint addConstraintAndValidate(Constraint constraint) throws PowerException;

	/**
	 * Creates a simple constraint
	 * 
	 * @param ess
	 * @param type
	 * @param phase
	 * @param pwr
	 * @param relationship
	 * @param value
	 * @return
	 */
	public Constraint createSimpleConstraint(String description, ManagedSymmetricEss ess, Phase phase, Pwr pwr,
			Relationship relationship, double value);

	/**
	 * Removes a Constraint.
	 * 
	 * @param type
	 * @param constraint
	 */
	public void removeConstraint(Constraint constraint);

	/**
	 * Gets the maximum possible Power under the active Constraints.
	 */
	public int getMaxPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr);

	/**
	 * Gets the minimum possible possible Power under the active Constraints.
	 */
	public int getMinPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr);

	/**
	 * Gets the Coefficient singleton for a specific combination of ess, phase and
	 * pwr
	 * 
	 * @param ess
	 * @param phase
	 * @param pwr
	 * @return
	 */
	Coefficient getCoefficient(ManagedSymmetricEss ess, Phase phase, Pwr pwr);

	/**
	 * Adjusts the given value so that it fits into Min/MaxActivePower.
	 * 
	 * @param value
	 * @return
	 */
	public default int fitValueIntoMinMaxActivePower(ManagedSymmetricEss ess, Phase phase, Pwr pwr, int value) {
		if (value > 0) {
			/*
			 * Discharge
			 */
			// fit into max possible discharge power
			int maxDischargePower = this.getMaxPower(ess, phase, pwr);
			if (value > maxDischargePower) {
				value = maxDischargePower;
			}

		} else {
			/*
			 * Charge
			 */
			// fit into max possible discharge power
			int maxChargePower = this.getMinPower(ess, phase, pwr);
			if (value < maxChargePower) {
				value = maxChargePower;
			}
		}
		return value;
	}
}
