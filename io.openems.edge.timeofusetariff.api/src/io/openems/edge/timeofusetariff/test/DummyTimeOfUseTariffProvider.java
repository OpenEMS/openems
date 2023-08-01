package io.openems.edge.timeofusetariff.test;

import java.time.ZonedDateTime;

import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public class DummyTimeOfUseTariffProvider implements TimeOfUseTariff {

	private TimeOfUsePrices prices;

	/**
	 * Builds a {@link DummyTimeOfUseTariffProvider} from hourly prices.
	 *
	 * @param hourlyPrices an array of 24 hourly prices.
	 * @param now          {@ZonedDateTime} given during test.
	 * @return a {@link DummyTimeOfUseTariffProvider}.
	 */
	public static DummyTimeOfUseTariffProvider hour1yPrices(ZonedDateTime now, Float... hourlyPrices) {
		return new DummyTimeOfUseTariffProvider(now, hourlyPrices);
	}

	/**
	 * Builds a {@link DummyTimeOfUseTariffProvider} from hourly prices.
	 *
	 * @param hourlyPrices an array of hourly prices.
	 * @param now          {@ZonedDateTime} given during test.
	 * @return a {@link DummyTimeOfUseTariffProvider}.
	 */
	public static DummyTimeOfUseTariffProvider quarterlyPrices(ZonedDateTime now, Float... hourlyPrices) {

		var quarterlyPrices = new Float[96];

		for (var i = 0; i < 24; i++) {
			quarterlyPrices[(i * 4)] = hourlyPrices[i];
			quarterlyPrices[(i * 4) + 1] = hourlyPrices[i];
			quarterlyPrices[(i * 4) + 2] = hourlyPrices[i];
			quarterlyPrices[(i * 4) + 3] = hourlyPrices[i];
		}

		return new DummyTimeOfUseTariffProvider(now, quarterlyPrices);
	}

	public DummyTimeOfUseTariffProvider(ZonedDateTime now, Float... quarterlyPrices) {
		this.setPrices(now, quarterlyPrices);
	}

	public void setPrices(ZonedDateTime now, Float... quarterlyPrices) {
		this.prices = new TimeOfUsePrices(now, quarterlyPrices);
	}

	@Override
	public TimeOfUsePrices getPrices() {
		// return empty TimeOfUsePrice if the dateTime is empty.
		if (this.prices.getUpdateTime() == null) {
			return TimeOfUsePrices.empty(ZonedDateTime.now());
		}
		return this.prices;
	}

}