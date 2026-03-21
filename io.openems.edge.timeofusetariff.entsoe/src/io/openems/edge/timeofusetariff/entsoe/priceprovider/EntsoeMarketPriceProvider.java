package io.openems.edge.timeofusetariff.entsoe.priceprovider;

public interface EntsoeMarketPriceProvider extends MarketPriceProvider {
	/**
	 * Returns the entsoe configuration of this price provider.
	 *
	 * @return {@link EntsoeConfiguration} reference
	 */
	public EntsoeConfiguration getConfig();
}
