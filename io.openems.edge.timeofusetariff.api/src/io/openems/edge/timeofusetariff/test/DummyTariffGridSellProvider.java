package io.openems.edge.timeofusetariff.test;

import java.time.Clock;
import java.time.Instant;
import java.util.stream.Stream;

import io.openems.edge.timeofusetariff.api.TariffGridSell;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public class DummyTariffGridSellProvider implements TariffGridSell {

	/**
	 * Builds a {@link DummyTariffGridSellProvider} with empty grid-sell prices.
	 *
	 * @param clock {@link Clock} given during test
	 * @return a {@link DummyTariffGridSellProvider}.
	 */
	public static DummyTariffGridSellProvider empty(Clock clock) {
		return new DummyTariffGridSellProvider(clock, TimeOfUsePrices.EMPTY_PRICES);
	}

	/**
	 * Builds a {@link DummyTariffGridSellProvider} from quarterly grid-sell prices.
	 *
	 * @param clock                   {@link Clock} given during test
	 * @param quarterlyGridSellPrices an array of quarterly grid-sell prices
	 * @return a {@link DummyTariffGridSellProvider}.
	 */
	public static DummyTariffGridSellProvider fromQuarterlyGridSellPrices(Clock clock,
			Double... quarterlyGridSellPrices) {
		return new DummyTariffGridSellProvider(clock,
				TimeOfUsePrices.from(Instant.now(clock), quarterlyGridSellPrices));
	}

	/**
	 * Builds a {@link DummyTariffGridSellProvider} from hourly grid-sell prices.
	 *
	 * @param clock                {@link Clock} given during test
	 * @param hourlyGridSellPrices an array of hourly grid-sell prices
	 * @return a {@link DummyTariffGridSellProvider}.
	 */
	public static DummyTariffGridSellProvider fromHourlyGridSellPrices(Clock clock, Double... hourlyGridSellPrices) {
		var quarterlyGridSellPrices = Stream.of(hourlyGridSellPrices) //
				.flatMap(v -> Stream.of(v, v, v, v)) //
				.toArray(Double[]::new);
		return new DummyTariffGridSellProvider(clock,
				TimeOfUsePrices.from(Instant.now(clock), quarterlyGridSellPrices));
	}

	private final Clock clock;
	private TimeOfUsePrices gridSellPrices;

	public DummyTariffGridSellProvider(Clock clock, TimeOfUsePrices gridSellPrices) {
		this.clock = clock;
		this.setGridSellPrices(gridSellPrices);
	}

	public void setGridSellPrices(TimeOfUsePrices gridSellPrices) {
		this.gridSellPrices = gridSellPrices;
	}

	@Override
	public TimeOfUsePrices getGridSellPrices() {
		return TimeOfUsePrices.from(Instant.now(this.clock), this.gridSellPrices);
	}
}