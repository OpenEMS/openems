package io.openems.edge.timeofusetariff.test;

import java.time.ZonedDateTime;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public class DummyTimeOfUseTariffProvider implements TimeOfUseTariff {

	private final TimeOfUsePrices prices;

	/**
	 * Builds a {@link DummyTimeOfUseTariffProvider} from hourly prices.
	 * 
	 * @param hourlyPrices an array of hourly prices
	 * @return a {@link DummyTimeOfUseTariffProvider}
	 */
	public static DummyTimeOfUseTariffProvider fromHourlyPrices(ZonedDateTime now, Float[] hourlyPrices) {
		Float[] quarterlyPrices = new Float[96];

		for (int i = 0; i < 24; i++) {
			quarterlyPrices[i] = hourlyPrices[i];
			quarterlyPrices[i + 1] = hourlyPrices[i];
			quarterlyPrices[i + 2] = hourlyPrices[i];
			quarterlyPrices[i + 3] = hourlyPrices[i];
		}

		return new DummyTimeOfUseTariffProvider(now, quarterlyPrices);
	}

	private DummyTimeOfUseTariffProvider(ZonedDateTime now, Float[] quarterlyPrices) {
		this.prices = new TimeOfUsePrices(now, quarterlyPrices);
	}

	@Override
	public TimeOfUsePrices getPrices() {
		return this.prices;
	}

}