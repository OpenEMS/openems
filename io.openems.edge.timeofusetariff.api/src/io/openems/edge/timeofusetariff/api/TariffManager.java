package io.openems.edge.timeofusetariff.api;

public interface TariffManager {

	/**
	 * Returns the day-ahead electricity prices for buying from the grid, for each
	 * 15-minute interval.
	 *
	 * <p>
	 * For example, if called at 10:05, the first value represents 10:00–10:15, the
	 * second 10:15–10:30, and so on.
	 *
	 * @return the grid-buy prices, or {@link TimeOfUsePrices#EMPTY_PRICES} if no
	 *         prices are available
	 */
	public TimeOfUsePrices getGridBuyDayAheadPrices();

	/**
	 * Returns the day-ahead electricity prices for selling to the grid, for each
	 * 15-minute interval.
	 *
	 * <p>
	 * For example, if called at 10:05, the first value represents 10:00–10:15, the
	 * second 10:15–10:30, and so on.
	 *
	 * @return the grid-sell prices, or {@link TimeOfUsePrices#EMPTY_PRICES} if no
	 *         prices are available
	 */
	public TimeOfUsePrices getGridSellDayAheadPrices();
}
