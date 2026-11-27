package io.openems.edge.timeofusetariff.spottariff.gridsell;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.openems.common.tariff.api.spottariff.dummy.DummySpotTariffProvider;
import io.openems.common.types.BiddingZone;
import io.openems.common.utils.TimeSpan;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.core.price.TariffManagerTestableImpl;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

class TariffGridSellSpotTariffTest {

	@Test
	void testSellTariff() throws Exception {
		final var clock = createDummyClock();
		final var dummySpotTariffProvider = new DummySpotTariffProvider();
		final var tariffData = dummySpotTariffProvider.setRandomTariffData(BiddingZone.GERMANY,
				TimeSpan.between(clock.instant(), clock.instant().plus(2, ChronoUnit.DAYS)),
				Currency.getInstance("EUR"));

		final var tariffManager = new TariffManagerTestableImpl();
		tariffManager.addSpotTariffProvider(dummySpotTariffProvider);

		final var gridSellTariffComponent = new TariffGridSellSpotTariffImpl();
		new ComponentTest(gridSellTariffComponent) //
				.addReference("tariffManager", tariffManager) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("tariffGridSell0") //
						.setBiddingZone(BiddingZone.GERMANY) //
						.setCalculateExpression("x * 2").build()) //

				.next(new AbstractComponentTest.TestCase("Successful price get") //
						.onAfterProcessImage(() -> {
							final var prices = gridSellTariffComponent.getGridSellPrices();
							assertNotEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
							assertEquals(tariffData.prices().getAt(clock.instant()) * 2, prices.getAt(clock.instant()),
									0.001);

						})) //
				.deactivate();
	}

	@Test
	void testSpotPriceUpdate() throws Exception {
		final var clock = createDummyClock();
		final var timeSpan = TimeSpan.between(clock.instant(), clock.instant().plus(2, ChronoUnit.DAYS));

		final var dummySpotTariffProvider = new DummySpotTariffProvider();
		dummySpotTariffProvider.setEmptyTariffData(BiddingZone.GERMANY);

		final var tariffManager = new TariffManagerTestableImpl();
		tariffManager.addSpotTariffProvider(dummySpotTariffProvider);

		final var gridSellTariffComponent = new TariffGridSellSpotTariffImpl();
		final var touTariffDataChangeCalls = new AtomicInteger(0);
		gridSellTariffComponent.getTariffData().subscribe(tariffData -> touTariffDataChangeCalls.incrementAndGet());

		new ComponentTest(gridSellTariffComponent) //
				.addReference("tariffManager", tariffManager) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("tariffGridSell0") //
						.setBiddingZone(BiddingZone.GERMANY) //
						.build()) //

				.next(new AbstractComponentTest.TestCase("Missing prices") //
						.onAfterProcessImage(() -> {
							final var prices = gridSellTariffComponent.getGridSellPrices();
							assertEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
						})) //

				.next(new AbstractComponentTest.TestCase("First price set") //
						.onBeforeProcessImage(() -> {
							dummySpotTariffProvider.setStaticTariffData(BiddingZone.GERMANY, timeSpan, 12.00,
									Currency.getInstance("EUR"));
						}) //
						.onAfterProcessImage(() -> {
							final var prices = gridSellTariffComponent.getGridSellPrices();
							assertNotEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
							assertEquals(12.00D, prices.getAt(clock.instant()), 0.001);
						})) //

				.next(new AbstractComponentTest.TestCase("Second price change") //
						.onBeforeProcessImage(() -> {
							dummySpotTariffProvider.setStaticTariffData(BiddingZone.GERMANY, timeSpan, 15.00,
									Currency.getInstance("EUR"));
						}) //
						.onAfterProcessImage(() -> {
							final var prices = gridSellTariffComponent.getGridSellPrices();
							assertEquals(15.00D, prices.getAt(clock.instant()), 0.001);
						}) //
				) //

				.deactivate();

		assertEquals(4, touTariffDataChangeCalls.get(),
				"Expected 4 tariff data change calls (1x empty, 1x first set, 1x second set, 1x deactivate)");
	}

}
