package io.openems.edge.energy.api;

import io.openems.common.utils.DoubleUtils.DoubleToDoubleFunction;

public enum RiskLevel {

	/**
	 * Less dependent on predictions.
	 */
	LOW(1.20, //
			p -> 1.0 + 0.5 * Math.pow(p, 3)/* Risk-Adjusted Consumption: Exponent 3; Max 1.2 */),

	/**
	 * Moderately dependent on predictions.
	 */
	MEDIUM(1.17, //
			p -> 1./* No Risk-Adjusted Consumption */),

	/**
	 * Heavily reliant on predictions.
	 */
	HIGH(1.10, //
			p -> 1./* No Risk-Adjusted Consumption */);

	/** Used to incorporate charge/discharge efficiency. */
	public final double efficiencyFactor;

	/**
	 * Used to increase consumption during peak-price periods.
	 * 
	 * <p>
	 * Applies a non-linear scaling factor based on a [0, 1] normalized price.
	 */
	public final DoubleToDoubleFunction consumptionFunction;

	private RiskLevel(double efficiencyFactor, DoubleToDoubleFunction consumptionFunction) {
		this.efficiencyFactor = efficiencyFactor;
		this.consumptionFunction = consumptionFunction;
	}
}
