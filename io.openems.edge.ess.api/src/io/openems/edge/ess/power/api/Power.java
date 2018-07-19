package io.openems.edge.ess.power.api;

import org.apache.commons.math3.optim.linear.Relationship;

import io.openems.edge.ess.api.ManagedSymmetricEss;

public interface Power {

	/**
	 * Adds a Constraint.
	 * 
	 * @param type
	 * @param constraint
	 */
	public Constraint addConstraint(Constraint constraint);

	/**
	 * Adds a Constraint using a ConstraintBuilder. Make sure to call 'build()' once
	 * finished.
	 * 
	 * @return
	 */
	public default ConstraintBuilder addConstraint() {
		return new ConstraintBuilder(this);
	}

	/**
	 * Adds a Constraint if the problem is still solvable afterwards.
	 * 
	 * @param type
	 * @param constraint
	 * @throws PowerException
	 */
	public Constraint addConstraintAndValidate(Constraint constraint) throws PowerException;

	/**
	 * Adds a simple constraint
	 * 
	 * @param ess
	 * @param type
	 * @param phase
	 * @param pwr
	 * @param relationship
	 * @param value
	 * @return
	 */
	public Constraint addSimpleConstraint(ManagedSymmetricEss ess, ConstraintType type, Phase phase, Pwr pwr,
			Relationship relationship, int value);

	/**
	 * Adds a simple constraint if the problem is still solvable afterwards.
	 * 
	 * @param ess
	 * @param type
	 * @param phase
	 * @param pwr
	 * @param relationship
	 * @param value
	 * @return
	 * @throws PowerException
	 */
	public Constraint addSimpleConstraintAndValidate(ManagedSymmetricEss ess, ConstraintType type, Phase phase, Pwr pwr,
			Relationship relationship, int value) throws PowerException;

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
}
