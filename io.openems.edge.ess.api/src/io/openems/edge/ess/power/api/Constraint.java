package io.openems.edge.ess.power.api;

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.math3.optim.linear.Relationship;

import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.ConstraintType;

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

	private Optional<Double> value;

	public Constraint(ConstraintType type, Coefficient[] coefficients, Relationship relationship, Double value) {
		this.type = type;
		this.coefficients = coefficients;
		this.relationship = relationship;
		this.value = Optional.ofNullable(value);
	}

	public Constraint(ConstraintType type, Coefficient[] coefficients, Relationship relationship, Integer value) {
		this(type, coefficients, relationship, value == null ? null : value.doubleValue());
	}

	@Override
	public String toString() {
		return "Constraint [coefficients=" + Arrays.toString(coefficients) + ", relationship=" + relationship.name()
				+ ", value=" + value + "]";
	}

	/**
	 * Whether this Constraint is Enabled and valid.
	 * 
	 * @return
	 */
	public boolean isEnabled() {
		return this.value.isPresent();
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

	public Optional<Double> getValue() {
		return this.value;
	}

	public void setDoubleValue(Double value) {
		this.value = Optional.ofNullable(value);
	}

	public void setIntValue(Integer value) {
		if (value == null) {
			this.value = Optional.empty();
		} else {
			this.value = Optional.ofNullable(value.doubleValue());
		}
	}
}
