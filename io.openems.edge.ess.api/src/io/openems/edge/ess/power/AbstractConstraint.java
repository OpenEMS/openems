package io.openems.edge.ess.power;

import org.apache.commons.math3.optim.linear.LinearConstraint;

public abstract class AbstractConstraint {

	private final double[] coefficients;

	public AbstractConstraint(int noOfCoefficients) {
		this.coefficients = new double[noOfCoefficients];
	}

	public abstract LinearConstraint[] getConstraints();

	protected double[] initializeCoefficients() {
		for (int i = 0; i < this.coefficients.length; i++) {
			this.coefficients[i] = 0;
		}
		return this.coefficients;
	}

}
