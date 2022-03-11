package io.openems.edge.timeofusetariff.api;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Provides a prediction for the next 24 h; one value per 15 minutes; 96 values
 * in total.
 */
@ProviderType
public interface TimeOfUseTariff {

	/**
	 * Gives electricity prices for the next 24 h; one value per 15 minutes; 96
	 * values in total.
	 *
	 * <p>
	 * E.g. if called at 10:05, the first value stands for 10:00 to 10:15; second
	 * value for 10:15 to 10:30.
	 *
	 * @return the prices, or null if no prices exist
	 */
	public TimeOfUsePrices getPrices();

}
