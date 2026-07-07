package io.openems.edge.timeofusetariff.api;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Provides quarterly grid-sell tariff prices.
 */
@ProviderType
public interface TariffGridSell {

	/**
	 * Returns electricity grid-sell prices, one value per 15-minute interval.
	 *
	 * <p>
	 * For example, if called at 10:05, the first value represents 10:00–10:15, the
	 * second 10:15–10:30, and so on.
	 *
	 * @return the grid-sell prices, or {@link TimeOfUsePrices#EMPTY_PRICES} if no
	 *         prices are known
	 */
	public TimeOfUsePrices getGridSellPrices();
}
