package io.openems.edge.ess.power.api;

import java.util.Arrays;

import org.apache.commons.math3.optim.linear.Relationship;

/**
 * Creates a constraint with following settings:
 * <ul>
 * <li>Relationship (EQ, GEQ, LEQ) as given in constructor
 * <li>Value as given in constructor
 * <li>Setting each coefficient, i.e.
 * 
 * <pre>
 * y = 1*p1 + 0*q1 * + 1*p2 + 0*q1 +...
 * </pre>
 * </ul>
 */
public class Constraint {

	private final ConstraintType type;
	private final Coefficient[] coefficients;
	private final Relationship relationship;
	
	private double value;

	public Constraint(ConstraintType type, Coefficient[] coefficients, Relationship relationship, double value) {
		this.type = type;
		this.coefficients = coefficients;
		this.relationship = relationship;
		this.value = value;
	}

	@Override
	public String toString() {
		return "Constraint [coefficients=" + Arrays.toString(coefficients) + ", relationship=" + relationship.name()
				+ ", value=" + value + "]";
	}

	public ConstraintType getType() {
		return type;
	}
	
	public Coefficient[] getCoefficients() {
		return coefficients;
	}

	public Relationship getRelationship() {
		return relationship;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}
}
