package io.openems.edge.timeofusetariff.api;

import io.openems.common.types.BiddingZone;

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
	TimeOfUsePrices getGridBuyDayAheadPrices();

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
	TimeOfUsePrices getGridSellDayAheadPrices();

	/**
	 * Returns a tracked Spot-Tariff service for the given bidding zone.
	 *
	 * <p>
	 * Callers must release the returned tracker via
	 * {@link #ungetSpotTariff(SpotTariffTracker)} when it is no longer needed.
	 *
	 * @param biddingZone the bidding zone
	 * @return the Spot-Tariff tracker for the bidding zone
	 */
	SpotTariffTracker getSpotTariff(BiddingZone biddingZone);

	/**
	 * Releases a previously acquired Spot-Tariff tracker.
	 *
	 * @param spotTariffTracker the tracker to release
	 */
	void ungetSpotTariff(SpotTariffTracker spotTariffTracker);
}
