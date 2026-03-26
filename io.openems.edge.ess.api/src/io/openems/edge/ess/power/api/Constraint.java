package io.openems.edge.ess.power.api;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Creates a constraint with following settings:.
 * 
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
public sealed class Constraint {

	private static final DecimalFormat VALUE_FORMAT = new DecimalFormat("0.#");

	public final String description;
	public final LinearCoefficient[] coefficients;
	public final Relationship relationship;
	public final double value;

	public Constraint(String description, LinearCoefficient[] coefficients, Relationship relationship, double value) {
		this.description = description;
		this.relationship = relationship;
		this.coefficients = coefficients;
		this.value = value;
	}

	public Constraint(String description, List<LinearCoefficient> coefficients, Relationship relationship,
			double value) {
		this(description, coefficients.toArray(new LinearCoefficient[coefficients.size()]), relationship, value);
	}

	/**
	 * Represents a simple Constraint with only one {@link Coefficient} set to '1'.
	 */
	public static final class Simple extends Constraint {

		public final LinearCoefficient coefficient;
		public final int value;

		public Simple(String description, Coefficient coefficient, Relationship relationship, int value) {
			this(description, new LinearCoefficient(coefficient, 1), relationship, value);
		}

		public Simple(String description, LinearCoefficient coefficient, Relationship relationship, int value) {
			super(description, new LinearCoefficient[] { coefficient }, relationship, value);
			this.coefficient = coefficient;
			this.value = value;
		}
	}

	@Override
	public String toString() {
		var b = new StringBuilder() //
				.append(String.format("%-30s", this.description));
		for (LinearCoefficient c : this.coefficients) {
			b.append(c.toString());
		}
		b //
				.append(" " + this.relationship.toString() + " ") //
				.append(VALUE_FORMAT.format(this.value));
		return b.toString();
	}
}
