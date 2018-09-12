package io.openems.edge.ess.power.api;

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
