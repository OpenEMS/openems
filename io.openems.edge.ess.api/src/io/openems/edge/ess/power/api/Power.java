package io.openems.edge.ess.power.api;

import org.apache.commons.math3.optim.linear.Relationship;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.coefficient.Coefficient;

public interface Power {

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
	public Constraint createSimpleConstraint(ManagedSymmetricEss ess, Phase phase, Pwr pwr, Relationship relationship,
			double value);

	/**
	 * Removes a Constraint.
	 * 
	 * @param type
	 * @param constraint
	 */
	public void removeConstraint(Constraint constraint);

	/**
	 * Gets the maximum possible total Active Power under the active Constraints.
	 */
	public int getMaxActivePower();

	/**
	 * Gets the minimum possible total Active Power under the active Constraints.
	 */
	public int getMinActivePower();

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

}
