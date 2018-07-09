package io.openems.edge.ess.power.api;

import org.apache.commons.math3.optim.linear.Relationship;

public class ConstraintBuilder {

	private final Power parent;
	private ConstraintType type = ConstraintType.CYCLE;
	private Coefficient[] coefficients;
	private Relationship relationship = Relationship.EQ;
	private double value = 0;

	/**
	 * Creates a Constraint. Make sure to call 'build()' to actually apply the
	 * constraint
	 * 
	 * Defaults are:
	 * <ul>
	 * <li>type = ConstraintType.CYCLE
	 * <li>relationship = Relationship.EQ
	 * <li>value = 0
	 * 
	 * @param parent
	 */
	public ConstraintBuilder(Power parent) {
		this.parent = parent;
	}

	public ConstraintBuilder type(ConstraintType type) {
		this.type = type;
		return this;
	}

	public ConstraintBuilder coefficients(Coefficient... coefficients) {
		this.coefficients = coefficients;
		return this;
	}

	public ConstraintBuilder relationship(Relationship relationship) {
		this.relationship = relationship;
		return this;
	}

	public ConstraintBuilder value(double value) {
		this.value = value;
		return this;
	}

	/**
	 * Builds the Constraint and adds it
	 */
	public Constraint build() {
		return parent.addConstraint(new Constraint(type, coefficients, relationship, value));
	}
}
