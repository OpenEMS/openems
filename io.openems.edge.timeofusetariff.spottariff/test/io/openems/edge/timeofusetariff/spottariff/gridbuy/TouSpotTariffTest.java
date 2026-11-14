package io.openems.edge.timeofusetariff.spottariff.gridbuy;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.parseToJsonArray;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.openems.common.tariff.api.spottariff.dummy.DummySpotTariffProvider;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.TariffData;
import io.openems.common.types.TariffType;
import io.openems.common.utils.TimeRangeValues;
import io.openems.common.utils.TimeSpan;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.core.price.TariffManagerTestableImpl;
import io.openems.edge.timeofusetariff.api.TariffComponent;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

class TouSpotTariffTest {

	@Test
	void testTouSpotTariff() throws Exception {
		final var clock = createDummyClock();
		final var dummySpotTariffProvider = new DummySpotTariffProvider();
		final var tariffData = dummySpotTariffProvider.setRandomTariffData(BiddingZone.GERMANY,
				TimeSpan.between(clock.instant(), clock.instant().plus(2, ChronoUnit.DAYS)),
				Currency.getInstance("EUR"));

		final var tariffManager = new TariffManagerTestableImpl();
		tariffManager.addSpotTariffProvider(dummySpotTariffProvider);

		final var gridBuyTariffComponent = new TouSpotTariffImpl();
		new ComponentTest(gridBuyTariffComponent) //
				.addReference("tariffManager", tariffManager) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("timeOfUseTariff0") //
						.setBiddingZone(BiddingZone.GERMANY) //
						.setAncillaryCosts(buildJsonObject() //
								.addProperty("dso", "BAYERNWERK") //
								.build() //
								.toString()) //
						.build()) //

				.next(new TestCase("Successful price get") //
						.onAfterProcessImage(() -> {
							final var prices = gridBuyTariffComponent.getPrices();
							assertNotEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
							assertEquals(tariffData.prices().getAt(clock.instant()), prices.getAt(clock.instant()));

						})) //
				.deactivate();
	}

	@Test
	void testWithAncillaryCostsAndCalculateExpression() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2026-02-02T00:00:00Z"));
		final var timeSpan = TimeSpan.between(//
				Instant.parse("2026-02-02T00:00:00Z"), //
				Instant.parse("2026-02-03T00:00:00Z") //
		);

		final var testSpotPrices = TimeRangeValues.builder(timeSpan, DurationUnit.ofMinutes(15), Double.class) //
				.setByTime(Instant.parse("2026-02-02T00:00:00Z"), 99.41D) //
				.setByTime(Instant.parse("2026-02-02T00:15:00Z"), 95.00D) //
				.setByTime(Instant.parse("2026-02-02T06:00:00Z"), 112.97D) //
				.setByTime(Instant.parse("2026-02-02T23:00:00Z"), 97.41D) //
				.fillMissingDataWithPreviousData() //
				.build();

		final var dummySpotTariffProvider = new DummySpotTariffProvider();
		dummySpotTariffProvider.setTariffData(BiddingZone.GERMANY,
				new TariffData(new TariffType.DayAheadEpexSpot(BiddingZone.GERMANY), testSpotPrices,
						Currency.getInstance("EUR"), clock.instant(), clock.instant(), ""));

		final var tariffManager = new TariffManagerTestableImpl();
		tariffManager.addSpotTariffProvider(dummySpotTariffProvider);

		final var schedule = parseToJsonArray(TestData.SCHEDULE);

		final var gridBuyTariffComponent = new TouSpotTariffImpl();
		new ComponentTest(gridBuyTariffComponent) //
				.addReference("tariffManager", tariffManager) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("timeOfUseTariff0") //
						.setBiddingZone(BiddingZone.GERMANY) //
						.setAncillaryCosts(buildJsonObject() //
								.addProperty("dso", "OTHER") //
								.add("schedule", schedule) //
								.build() //
								.toString()) //
						.setCalculateExpression("(x + y) * 1.19") //
						.build()) //

				.next(new TestCase("Successful price get") //
						.onAfterProcessImage(() -> {
							final var prices = gridBuyTariffComponent.getPrices();
							assertNotEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
							assertEquals((99.41 + (0.1 * 10)) * 1.19,
									prices.getAt(Instant.parse("2026-02-02T00:00:00Z")), 0.001);
							assertEquals((95.00 + (0.1 * 10)) * 1.19,
									prices.getAt(Instant.parse("2026-02-02T00:15:00Z")), 0.001);
							assertEquals((112.97 + (0.2 * 10)) * 1.19,
									prices.getAt(Instant.parse("2026-02-02T06:00:00Z")), 0.001);
							assertEquals((97.41 + (0.3 * 10)) * 1.19,
									prices.getAt(Instant.parse("2026-02-02T23:00:00Z")), 0.001);

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

		final var gridBuyTariffComponent = new TouSpotTariffImpl();
		final var touTariffDataChangeCalls = new AtomicInteger(0);
		gridBuyTariffComponent.getTariffData().subscribe(tariffData -> touTariffDataChangeCalls.incrementAndGet());

		new ComponentTest(gridBuyTariffComponent) //
				.addReference("tariffManager", tariffManager) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("timeOfUseTariff0") //
						.setBiddingZone(BiddingZone.GERMANY) //
						.setAncillaryCosts(buildJsonObject() //
								.addProperty("dso", "BAYERNWERK") //
								.build() //
								.toString()) //
						.build()) //

				.next(new TestCase("Missing prices") //
						.output(TariffComponent.ChannelId.NO_PRICES, true) //
						.onAfterProcessImage(() -> {
							final var prices = gridBuyTariffComponent.getPrices();
							assertEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
						})) //

				.next(new TestCase("First price set") //
						.output(TariffComponent.ChannelId.NO_PRICES, false) //
						.onBeforeProcessImage(() -> {
							dummySpotTariffProvider.setStaticTariffData(BiddingZone.GERMANY, timeSpan, 12.00,
									Currency.getInstance("EUR"));
						}) //
						.onAfterProcessImage(() -> {
							final var prices = gridBuyTariffComponent.getPrices();
							assertNotEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
							assertEquals(12.00D, prices.getAt(clock.instant()), 0.001);
						})) //

				.next(new TestCase("Second price change") //
						.onBeforeProcessImage(() -> {
							dummySpotTariffProvider.setStaticTariffData(BiddingZone.GERMANY, timeSpan, 15.00,
									Currency.getInstance("EUR"));
						}) //
						.onAfterProcessImage(() -> {
							final var prices = gridBuyTariffComponent.getPrices();
							assertEquals(15.00D, prices.getAt(clock.instant()), 0.001);
						}) //
				) //

				.deactivate();

		assertEquals(4, touTariffDataChangeCalls.get(),
				"Expected 4 tariff data change calls (1x empty, 1x first set, 1x second set, 1x deactivate)");
	}

}
