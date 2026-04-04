package io.openems.edge.timeofusetariff.test;

import io.openems.edge.timeofusetariff.api.TariffGridSell;
import io.openems.edge.timeofusetariff.api.TariffManager;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public class DummyTariffManager implements TariffManager {

	private TimeOfUseTariff tariffGridBuyProvider;
	private TariffGridSell tariffGridSellProvider;

	public DummyTariffManager() {
	}

	/**
	 * Set the {@link TimeOfUseTariff}.
	 *
	 * @param tariffGridBuyProvider the {@link TimeOfUseTariff}
	 * @return myself
	 */
	public DummyTariffManager withTariffGridBuyProvider(TimeOfUseTariff tariffGridBuyProvider) {
		this.tariffGridBuyProvider = tariffGridBuyProvider;
		return this;
	}

	/**
	 * Set the {@link TariffGridSell}.
	 *
	 * @param tariffGridSellProvider the {@link TariffGridSell}
	 * @return myself
	 */
	public DummyTariffManager withTariffGridSellProvider(TariffGridSell tariffGridSellProvider) {
		this.tariffGridSellProvider = tariffGridSellProvider;
		return this;
	}

	@Override
	public TimeOfUsePrices getGridBuyDayAheadPrices() {
		return this.tariffGridBuyProvider.getPrices();
	}

	@Override
	public TimeOfUsePrices getGridSellDayAheadPrices() {
		return this.tariffGridSellProvider.getGridSellPrices();
	}
}
