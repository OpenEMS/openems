package io.openems.edge.ess.power.api;

import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.Relationship;

/**
 * Wraps a LinearConstraint into an AbstractConstraint
 */
public class LinearConstraintWrapper extends AbstractConstraint {

	private final LinearConstraint constraint;

	public LinearConstraintWrapper(LinearConstraint constraint, String note) {
		super(0, note);
		this.constraint = constraint;
	}

	public LinearConstraintWrapper(double[] coefficients, Relationship relationship, double value, String note) {
		super(0, note);
		this.constraint = new LinearConstraint(coefficients, relationship, value);
	}

	@Override
	public LinearConstraint[] getConstraints() {
		return new LinearConstraint[] { this.constraint };
	}

}
