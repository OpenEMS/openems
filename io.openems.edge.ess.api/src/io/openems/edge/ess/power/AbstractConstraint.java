package io.openems.edge.ess.power;

import org.apache.commons.math3.optim.linear.LinearConstraint;

public abstract class AbstractConstraint {

	private final String note;
	private final double[] coefficients;

	public AbstractConstraint(int noOfCoefficients, String note) {
		this.coefficients = new double[noOfCoefficients];
		this.note = note;
	}

	public abstract LinearConstraint[] getConstraints();

	protected double[] initializeCoefficients() {
		for (int i = 0; i < this.coefficients.length; i++) {
			this.coefficients[i] = 0;
		}
		return this.coefficients;
	}

	@Override
	public String toString() {
		LinearConstraint[] constraints = this.getConstraints();
		StringBuilder b = new StringBuilder();
		for (int j = 0; j < constraints.length; j++) {
			LinearConstraint constraint = constraints[j];
			b.append(Utils.linearConstraintToString(constraint, this.note));
			if (j < constraints.length - 1) {
				b.append(String.format("%n"));
			}
		}
		return b.toString();
	}
}
