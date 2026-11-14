package io.openems.edge.timeofusetariff.manual.eeg2025.gridsell;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.openems.common.tariff.api.spottariff.dummy.DummySpotTariffProvider;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.TariffData;
import io.openems.common.types.TariffType;
import io.openems.common.utils.TimeRangeValues;
import io.openems.common.utils.TimeSpan;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.core.price.TariffManagerTestableImpl;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

class TariffManualEeg2025GridSellTest {

	private static final double FIX_SELL_PRICE_KWH = 23.0D;
	private static final double FIX_SELL_PRICE = FIX_SELL_PRICE_KWH * 10;

	@Test
	void testWithSpotPriceUpdate() throws Exception {
		final var clock = createDummyClock();
		final var timeSpan = TimeSpan.between(clock.instant(), clock.instant().plus(2, ChronoUnit.DAYS));

		final var dummySpotTariffProvider = new DummySpotTariffProvider();
		dummySpotTariffProvider.setEmptyTariffData(BiddingZone.GERMANY);

		final var tariffManager = new TariffManagerTestableImpl();
		tariffManager.addSpotTariffProvider(dummySpotTariffProvider);

		final var eeg2025component = new TariffManualEeg2025GridSellImpl();
		final var touTariffDataChangeCalls = new AtomicInteger(0);
		eeg2025component.getTariffData().subscribe(tariffData -> touTariffDataChangeCalls.incrementAndGet());

		new ComponentTest(eeg2025component) //
				.addReference("tariffManager", tariffManager) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("tariffGridSell0") //
						.setFixedGridSellPrice(FIX_SELL_PRICE_KWH) //
						.build()) //

				.next(new AbstractComponentTest.TestCase("Missing prices") //
						.onAfterProcessImage(() -> {
							final var prices = eeg2025component.getGridSellPrices();
							assertEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
						})) //

				.next(new AbstractComponentTest.TestCase("First price set") //
						.onBeforeProcessImage(() -> {
							final var testSpotPrices = TimeRangeValues
									.builder(timeSpan, DurationUnit.ofMinutes(15), Double.class) //
									.setByPosition(0, 12.00) //
									.setByPosition(1, 13.00) //
									.setByPosition(2, -02.00) //
									.setByPosition(3, 0.00) //
									.build();

							final var tariffData = new TariffData(new TariffType.DayAheadEpexSpot(BiddingZone.GERMANY),
									testSpotPrices, Currency.getInstance("EUR"), clock.instant(), clock.instant(), "");

							dummySpotTariffProvider.setTariffData(BiddingZone.GERMANY, tariffData);
						}) //
						.onAfterProcessImage(() -> {
							final var prices = eeg2025component.getGridSellPrices().asArray();

							assertEquals(FIX_SELL_PRICE, prices[0], 0.001);
							assertEquals(FIX_SELL_PRICE, prices[1], 0.001);
							assertEquals(0.0, prices[2], 0.001);
							assertEquals(0.0, prices[3], 0.001);
						})) //

				.next(new AbstractComponentTest.TestCase("Second price change") //
						.onBeforeProcessImage(() -> {
							final var testSpotPrices = TimeRangeValues
									.builder(timeSpan, DurationUnit.ofMinutes(15), Double.class) //
									.setByPosition(0, 12.00) //
									.setByPosition(1, 13.00) //
									.setByPosition(2, 11.00) //
									.setByPosition(3, -2.00) //
									.build();

							final var tariffData = new TariffData(new TariffType.DayAheadEpexSpot(BiddingZone.GERMANY),
									testSpotPrices, Currency.getInstance("EUR"), clock.instant(), clock.instant(), "");

							dummySpotTariffProvider.setTariffData(BiddingZone.GERMANY, tariffData);
						}) //
						.onAfterProcessImage(() -> {
							final var prices = eeg2025component.getGridSellPrices().asArray();

							assertEquals(FIX_SELL_PRICE, prices[0], 0.001);
							assertEquals(FIX_SELL_PRICE, prices[1], 0.001);
							assertEquals(FIX_SELL_PRICE, prices[2], 0.001);
							assertEquals(0.0, prices[3], 0.001);
						})) //

				.deactivate();

		assertEquals(4, touTariffDataChangeCalls.get(),
				"Expected 4 tariff data change calls (1x empty, 1x first set, 1x second set, 1x deactivate)");
	}
}
