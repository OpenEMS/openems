package io.openems.edge.timeofusetariff.entsoe.priceprovider;

import io.openems.common.types.MarketPriceData;
import io.openems.common.utils.BehaviorSubject;

public interface MarketPriceProvider {
	/**
	 * Triggers a price update. The method is running asynchronous, so it will take
	 * a few seconds before the new prices are set.
	 */
	void triggerPriceUpdate();

	/**
	 * Returns the current stored market prices of the last successful price fetch.
	 * @return Market prices
	 */
	BehaviorSubject<MarketPriceData> getMarketPrices();

	/**
	 * Returns the result of the last price fetch.
	 * @return Update state
	 */
	BehaviorSubject<MarketPriceUpdateEvent> getUpdateState();
}
