package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.common.currency.Currency.EUR;
import static io.openems.edge.timeofusetariff.entsoe.Utils.parseToSchedule;
import static java.time.LocalTime.MIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;

import org.junit.Test;

import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.timeofusetariff.api.AncillaryCosts.GridFee;
import io.openems.edge.timeofusetariff.api.GermanDSO;
import io.openems.edge.timeofusetariff.api.TouManualHelper;

public class TouEntsoeTest {
	private static final long FULL_DAY_MINUTES = 1440;
	private static final ZoneId GERMAN_ZONE_ID = ZoneId.of("Europe/Berlin");
	private static final ZoneId ZONE_ID = ZoneId.systemDefault();
	private static final int YEAR = 2026;

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		var entsoe = new TouEntsoeImpl();
		var dummyMeta = new DummyMeta("foo0") //
				.withCurrency(EUR);
		new ComponentTest(entsoe) //
				.addReference("meta", dummyMeta) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("httpBridgeFactory",
						DummyBridgeHttpFactory.ofBridgeImpl(DummyBridgeHttpFactory::dummyEndpointFetcher,
								DummyBridgeHttpFactory::dummyBridgeHttpExecutor)) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("tou0") //
						.setSecurityToken("") //
						.setBiddingZone(BiddingZone.GERMANY) //
						.setResolution(Resolution.HOURLY) //
						.setAncillaryCosts(buildJsonObject() //
								.addProperty("dso", "BAYERNWERK") //
								.build() //
								.toString())
						.build());
	}

	@Test
	public void testAllDsoSchedules() {
		for (final var dso : GermanDSO.values()) {
			final var gridFee = dso.gridFee;
			for (final var dateRange : gridFee.dateRanges()) {
				var currentDay = dateRange.start();
				while (!currentDay.isAfter(dateRange.end())) {
					// Sort time ranges by start time
					final var timeRanges = dateRange.timeRanges() //
							.stream() //
							.sorted(Comparator.comparing(GridFee.TimeRange::start)) //
							.toList();

					// Check coverage and overlaps
					var lastEnd = MIN;
					long totalMinutes = 0;

					for (final var tr : timeRanges) {
						final var start = tr.start();
						final var end = tr.end();

						// Check for gaps or overlaps
						assertTrue("[" + dso.name() + "] Gap/overlap on " + currentDay + " between " + lastEnd + " and "
								+ start, !lastEnd.isAfter(start));

						// Calculate duration in minutes
						totalMinutes += calculateMinutes(start, end);
						lastEnd = end;
					}

					// Verify entire day is covered (24 hours)
					assertTrue("[" + dso.name() + "] Incomplete day coverage on " + currentDay + ": " + totalMinutes
							+ " minutes", totalMinutes == FULL_DAY_MINUTES);

					// Verify tariff consistency: low < standard < high
					if (dateRange.lowTariff() > 0 || dateRange.highTariff() > 0) {
						assertTrue("[" + dso.name() + "] Tariff violation: low < standard < high",
								dateRange.lowTariff() < dateRange.standardTariff()
										&& dateRange.standardTariff() < dateRange.highTariff());
					}
					currentDay = currentDay.plusDays(1);
				}
			}
		}
	}

	private static long calculateMinutes(LocalTime start, LocalTime end) {

		final var difference = Duration.between(start, end);

		if (!difference.isPositive()) {
			return difference.plusDays(1).toMinutes();
		}
		return difference.toMinutes();
	}

	private TouManualHelper buildHelper(String dso, String clockTime) throws OpenemsNamedException {
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", dso) //
				.build() //
				.toString();

		var clock = new TimeLeapClock(Instant.parse(clockTime), GERMAN_ZONE_ID);
		var schedule = parseToSchedule(clock, BiddingZone.GERMANY, ancillaryCosts, msg -> fail(msg));

		return new TouManualHelper(clock, schedule, 0.0);
	}

	private static ZonedDateTime toZonedDateTime(int year, int month, int day, int hour, int minute) {
		return LocalDate.of(year, month, day).atTime(hour, minute).atZone(ZONE_ID);
	}

	@Test
	public void testStandardTariffOnJuly15At2PM() throws OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", "2026-07-15T00:00:00Z");
		final var testTime = toZonedDateTime(2026, 7, 15, 14, 0);
		var expectedPrice = GermanDSO.BAYERNWERK.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_ODR", "2026-07-15T00:00:00Z");

		expectedPrice = GermanDSO.NETZE_ODR.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testLowTariffOnOctober20At3AM() throws OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", "2026-10-20T00:00:00Z");
		final var testTime = toZonedDateTime(2026, 10, 20, 3, 0);
		var expectedPrice = GermanDSO.BAYERNWERK.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_BW", "2026-10-20T00:00:00Z");
		expectedPrice = GermanDSO.NETZE_BW.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("MIT_NETZ", "2026-10-20T00:00:00Z");
		expectedPrice = GermanDSO.MIT_NETZ.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("E_DIS", "2026-10-20T00:00:00Z");
		expectedPrice = GermanDSO.E_DIS.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("LEW", "2026-10-20T00:00:00Z");
		expectedPrice = GermanDSO.LEW.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_ODR", "2026-10-20T00:00:00Z");
		expectedPrice = GermanDSO.NETZE_ODR.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testHighTariffOnOctober20At5PM() throws OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", "2026-10-20T00:00:00Z");
		final var testTime = toZonedDateTime(2026, 10, 20, 17, 0);
		var expectedPrice = GermanDSO.BAYERNWERK.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_BW", "2026-10-20T00:00:00Z");
		expectedPrice = GermanDSO.NETZE_BW.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("MIT_NETZ", "2026-10-20T00:00:00Z");
		expectedPrice = GermanDSO.MIT_NETZ.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("WEST_NETZ", "2026-10-20T00:00:00Z");
		expectedPrice = GermanDSO.WEST_NETZ.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("AVACON", "2026-10-20T00:00:00Z");
		expectedPrice = GermanDSO.AVACON.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_ODR", "2026-10-20T00:00:00Z");
		expectedPrice = GermanDSO.NETZE_ODR.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testStandardTariffOnApril10At12PM() throws OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", YEAR + "-04-10T00:00:00Z");
		var testTime = toZonedDateTime(YEAR, 4, 10, 12, 0);
		var expectedPrice = GermanDSO.BAYERNWERK.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_BW", YEAR + "-04-10T00:00:00Z");
		expectedPrice = GermanDSO.NETZE_BW.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_BW", YEAR + "-04-09T00:00:00Z");
		testTime = toZonedDateTime(YEAR, 4, 10, 0, 0);
		expectedPrice = GermanDSO.NETZE_BW.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_ODR", YEAR + "-04-09T00:00:00Z");
		expectedPrice = GermanDSO.NETZE_ODR.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testEmptyScheduleReturnsZeroPrice() throws OpenemsNamedException {
		var clock = new TimeLeapClock(Instant.parse(YEAR + "-02-01T00:00:00Z"), GERMAN_ZONE_ID);
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", "other") //
				.add("schedule", buildJsonArray() //
						.build()) // empty
				.build() //
				.toString();

		var schedule = parseToSchedule(clock, BiddingZone.GERMANY, ancillaryCosts, msg -> fail(msg));
		var helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime = toZonedDateTime(YEAR, 2, 1, 10, 0);
		assertEquals(0.0, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testMissingSchedule() throws OpenemsNamedException {
		var clock = new TimeLeapClock(Instant.parse(YEAR + "-02-01T00:00:00Z"), GERMAN_ZONE_ID);
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", "other") // simulate missing schedule
				.build() //
				.toString();

		var schedule = parseToSchedule(clock, BiddingZone.GERMANY, ancillaryCosts, msg -> fail(msg));
		var helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime = toZonedDateTime(YEAR, 2, 1, 10, 0);
		assertEquals(0.0, helper.getPrices().getAt(testTime), 0.01);
	}
}
