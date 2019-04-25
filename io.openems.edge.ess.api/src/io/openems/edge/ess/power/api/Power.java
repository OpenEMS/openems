package io.openems.edge.ess.power.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.api.ManagedSymmetricEss;

public interface Power {

	public final static Logger log = LoggerFactory.getLogger(Power.class);

	public static Constraint[] NO_CONSTRAINTS = new Constraint[] {};

	/**
	 * Adds a Constraint.
	 * 
	 * @param constraint
	 */
	public Constraint addConstraint(Constraint constraint);

	/**
	 * Adds a Constraint if the problem is still solvable afterwards.
	 * 
	 * @param constraint
	 * @throws PowerException
	 */
	public Constraint addConstraintAndValidate(Constraint constraint) throws PowerException;

	/**
	 * Creates a simple constraint
	 * 
	 * @param description
	 * @param ess
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
	 * Adjusts the given value so that it fits into Min/MaxPower.
	 * 
	 * @param value the target value
	 * @return a value that fits into Min/MaxPower
	 */
	public default int fitValueIntoMinMaxPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr, int value) {
		/*
		 * Discharge
		 */
		// fit into max possible discharge power
		int maxDischargePower = this.getMaxPower(ess, phase, pwr);
		if (value > maxDischargePower) {
			Power.log.info("Reducing power from [" + value + "] to [" + maxDischargePower + "] for [" + ess.id()
					+ pwr.getSymbol() + phase.getSymbol() + "]");
			value = maxDischargePower;
		}

		/*
		 * Charge
		 */
		// fit into max possible discharge power
		int maxChargePower = this.getMinPower(ess, phase, pwr);
		if (value < maxChargePower) {
			Power.log.info("Reducing power from [" + value + "] to [" + (maxChargePower * -1) + "] for [" + ess.id()
					+ pwr.getSymbol() + phase.getSymbol() + "]");
			value = maxChargePower;
		}
		return value;
	}
}
