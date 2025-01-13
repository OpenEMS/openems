package io.openems.edge.timeofusetariff.api;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Provides quarterly Time-of-Use Tariff prices.
 */
@ProviderType
public interface TimeOfUseTariff {

	/**
	 * Gives electricity prices; one value per 15 minutes.
	 *
	 * <p>
	 * E.g. if called at 10:05, the first value stands for 10:00 to 10:15; second
	 * value for 10:15 to 10:30.
	 *
	 * @return the prices; {@link TimeOfUsePrices#empty(java.time.ZonedDateTime)} if
	 *         no prices are known
	 */
	public TimeOfUsePrices getPrices();

}
