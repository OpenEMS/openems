package io.openems.edge.timeofusetariff.test;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public class DummyTimeOfUseTariffProvider implements TimeOfUseTariff {

	/**
	 * Builds a {@link DummyTimeOfUseTariffProvider} with empty prices.
	 *
	 * @param clock {@Clock} given during test
	 * @return a {@link DummyTimeOfUseTariffProvider}.
	 */
	public static DummyTimeOfUseTariffProvider empty(Clock clock) {
		return new DummyTimeOfUseTariffProvider(clock, TimeOfUsePrices.EMPTY_PRICES);
	}

	/**
	 * Builds a {@link DummyTimeOfUseTariffProvider} from quarterly prices.
	 *
	 * @param clock           {@Clock} given during test
	 * @param quarterlyPrices an array of quarterly prices
	 * @return a {@link DummyTimeOfUseTariffProvider}.
	 */
	public static DummyTimeOfUseTariffProvider fromQuarterlyPrices(Clock clock, Double... quarterlyPrices) {
		return new DummyTimeOfUseTariffProvider(clock, TimeOfUsePrices.from(ZonedDateTime.now(clock), quarterlyPrices));
	}

	/**
	 * Builds a {@link DummyTimeOfUseTariffProvider} from hourly prices.
	 *
	 * @param clock        {@Clock} given during test
	 * @param hourlyPrices an array of hourly prices
	 * @return a {@link DummyTimeOfUseTariffProvider}.
	 */
	public static DummyTimeOfUseTariffProvider fromHourlyPrices(Clock clock, Double... hourlyPrices) {
		var quarterlyPrices = Stream.of(hourlyPrices) //
				.flatMap(v -> Stream.of(v, v, v, v)) //
				.toArray(Double[]::new);
		return new DummyTimeOfUseTariffProvider(clock, TimeOfUsePrices.from(ZonedDateTime.now(clock), quarterlyPrices));
	}

	private final Clock clock;
	private TimeOfUsePrices prices;

	public DummyTimeOfUseTariffProvider(Clock clock, TimeOfUsePrices prices) {
		this.clock = clock;
		this.setPrices(prices);
	}

	public void setPrices(TimeOfUsePrices prices) {
		this.prices = prices;
	}

	@Override
	public TimeOfUsePrices getPrices() {
		return TimeOfUsePrices.from(ZonedDateTime.now(this.clock), this.prices);
	}

}