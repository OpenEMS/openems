package io.openems.edge.energy.api;

import io.openems.common.utils.DoubleUtils.DoubleToDoubleFunction;

public enum Environment {

	/**
	 * Beta / Test environment, can be used for testing features.
	 */
	BETA(p -> 1.0 + 0.2 * Math.pow(p, 3)),

	/**
	 * Production environment, main live deployment.
	 */
	PRODUCTION(p -> 1.0 + 0.2 * Math.pow(p, 3)),

	/**
	 * Only for testing, not used in production.
	 */
	TEST(p -> 1.),

	;

	/**
	 * Used to increase consumption during peak-price periods.
	 * 
	 * <p>
	 * Applies a non-linear scaling factor based on a [0, 1] normalized price.
	 */
	public final DoubleToDoubleFunction consumptionFunction;

	private Environment(DoubleToDoubleFunction consumptionFunction) {
		this.consumptionFunction = consumptionFunction;
	}
}
