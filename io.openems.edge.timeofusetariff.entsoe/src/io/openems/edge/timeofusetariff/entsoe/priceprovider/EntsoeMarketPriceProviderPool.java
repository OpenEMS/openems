package io.openems.edge.timeofusetariff.entsoe.priceprovider;

public interface EntsoeMarketPriceProviderPool {
	/**
	 * Returns a provider instance for the given configuration. If there is already
	 * a existing instance for the same configuration, the existing instance is
	 * returned. If not, a new instance is created and returned.
	 *
	 * @param config Entsoe configuration
	 * @return Entsoe price provider
	 */
	public EntsoeMarketPriceProvider get(EntsoeConfiguration config);

	/**
	 * Returns the instance to the given provider. Don't call this method twice! If
	 * there is no reference to the provider after this unget, the provider gets
	 * unloaded.
	 * 
	 * @param provider Provider to unget
	 */
	public void unget(EntsoeMarketPriceProvider provider);
}
