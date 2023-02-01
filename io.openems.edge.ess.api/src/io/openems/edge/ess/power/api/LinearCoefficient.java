package io.openems.edge.ess.power.api;

import java.text.DecimalFormat;

public class LinearCoefficient {

	private static final DecimalFormat VALUE_FORMAT = new DecimalFormat("+0.#;-#");

	protected final Coefficient coefficient;
	protected final double value;

	public LinearCoefficient(Coefficient coefficient, double value) {
		this.coefficient = coefficient;
		this.value = value;
	}

	public Coefficient getCoefficient() {
		return this.coefficient;
	}

	public double getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		if (this.value == 1) {
			return "+" + this.coefficient.toString();
		}
		if (this.value == -1) {
			return "-" + this.coefficient.toString();
		} else {
			return VALUE_FORMAT.format(this.value) + "*" + this.coefficient.toString();
		}
	}
}
