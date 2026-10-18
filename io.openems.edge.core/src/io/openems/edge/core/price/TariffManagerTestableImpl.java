package io.openems.edge.core.price;

import java.util.ArrayList;

import io.openems.common.services.currency.DummyCurrencyExchange;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.edge.common.test.DummyMeta;

public class TariffManagerTestableImpl extends TariffManagerImpl {
	public TariffManagerTestableImpl() {
		this.spotTariffProviders = new ArrayList<>();
		this.meta = new DummyMeta() //
				.withCurrency(io.openems.edge.common.currency.Currency.EUR);
		this.currencyExchange = new DummyCurrencyExchange();
	}

	/**
	 * Adds spot tariff provider to the tariff manager.
	 *
	 * @param provider Provider to add, must not be null.
	 */
	public void addSpotTariffProvider(SpotTariffProvider provider) {
		this.spotTariffProviders.add(provider);
		this.bindSpotTariffProvider(provider);
	}

	/**
	 * Removes spot tariff provider from the tariff manager.
	 *
	 * @param provider Provider to remove, must be added before with
	 *                 addSpotTariffProvider
	 */
	public void removeSpotTariffProvider(SpotTariffProvider provider) {
		if (this.spotTariffProviders.remove(provider)) {
			this.unbindSpotTariffProvider(provider);
		}
	}
}
