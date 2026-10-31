package io.openems.edge.timeofusetariff.spottariff.gridbuy;

import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.parseToJsonArray;
import static java.time.LocalTime.MIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;

import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.timeofusetariff.api.AncillaryCosts;
import io.openems.edge.timeofusetariff.api.GermanDSO;
import io.openems.edge.timeofusetariff.api.TouManualHelper;

class ScheduleTest {
	private static final long FULL_DAY_MINUTES = 1440;
	private static final ZoneId GERMAN_ZONE_ID = ZoneId.of("Europe/Berlin");
	private static final ZoneId ZONE_ID = ZoneId.systemDefault();
	private static final int YEAR = 2026;

	@Test
	void testAllDsoSchedules() {
		for (final var dso : GermanDSO.values()) {
			final var gridFee = dso.gridFee;
			for (final var dateRange : gridFee.dateRanges()) {
				var currentDay = dateRange.start();
				while (!currentDay.isAfter(dateRange.end())) {
					// Sort time ranges by start time
					final var timeRanges = dateRange.timeRanges() //
							.stream() //
							.sorted(Comparator.comparing(AncillaryCosts.GridFee.TimeRange::start)) //
							.toList();

					// Check coverage and overlaps
					var lastEnd = MIN;
					long totalMinutes = 0;

					for (final var tr : timeRanges) {
						final var start = tr.start();
						final var end = tr.end();

						// Check for gaps or overlaps
						assertTrue(!lastEnd.isAfter(start), "[" + dso.name() + "] Gap/overlap on " + currentDay
								+ " between " + lastEnd + " and " + start);

						// Calculate duration in minutes
						totalMinutes += calculateMinutes(start, end);
						lastEnd = end;
					}

					// Verify entire day is covered (24 hours)
					assertEquals(FULL_DAY_MINUTES, totalMinutes, "[" + dso.name() + "] Incomplete day coverage on "
							+ currentDay + ": " + totalMinutes + " minutes");

					// Verify tariff consistency: low < standard < high
					if (dateRange.lowTariff() > 0 || dateRange.highTariff() > 0) {
						assertTrue(
								dateRange.lowTariff() < dateRange.standardTariff()
										&& dateRange.standardTariff() < dateRange.highTariff(),
								"[" + dso.name() + "] Tariff violation: low < standard < high");
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

	private TouManualHelper buildHelper(String dso, String clockTime) throws OpenemsError.OpenemsNamedException {
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", dso) //
				.build() //
				.toString();

		var clock = new TimeLeapClock(Instant.parse(clockTime), GERMAN_ZONE_ID);
		var schedule = AncillaryCosts.parseForGermany(clock, ancillaryCosts);

		return new TouManualHelper(clock, schedule, 0.0);
	}

	private static ZonedDateTime toZonedDateTime(int year, int month, int day, int hour, int minute) {
		return LocalDate.of(year, month, day).atTime(hour, minute).atZone(ZONE_ID);
	}

	@Test
	void testStandardTariffOnJuly15At2PM() throws OpenemsError.OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", YEAR + "-07-15T00:00:00Z");
		final var testTime = toZonedDateTime(YEAR, 7, 15, 14, 0);
		var expectedPrice = GermanDSO.BAYERNWERK.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_ODR", YEAR + "-07-15T00:00:00Z");

		expectedPrice = GermanDSO.NETZE_ODR.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	void testLowTariffOnOctober20At3AM() throws OpenemsError.OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", YEAR + "-10-20T00:00:00Z");
		final var testTime = toZonedDateTime(YEAR, 10, 20, 3, 0);
		var expectedPrice = GermanDSO.BAYERNWERK.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_BW", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.NETZE_BW.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("MIT_NETZ", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.MIT_NETZ.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("E_DIS", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.E_DIS.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("LEW", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.LEW.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_ODR", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.NETZE_ODR.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	void testHighTariffOnOctober20At5PM() throws OpenemsError.OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", YEAR + "-10-20T00:00:00Z");
		final var testTime = toZonedDateTime(YEAR, 10, 20, 17, 0);
		var expectedPrice = GermanDSO.BAYERNWERK.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_BW", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.NETZE_BW.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("MIT_NETZ", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.MIT_NETZ.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("WEST_NETZ", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.WEST_NETZ.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("AVACON", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.AVACON.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_ODR", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.NETZE_ODR.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	void testStandardTariffOnApril10At12PM() throws OpenemsError.OpenemsNamedException {
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
	void testEmptyScheduleReturnsZeroPrice() throws OpenemsError.OpenemsNamedException {
		var clock = new TimeLeapClock(Instant.parse(YEAR + "-02-01T00:00:00Z"), GERMAN_ZONE_ID);
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", "other") //
				.add("schedule", buildJsonArray() //
						.build()) // empty
				.build() //
				.toString();

		var schedule = AncillaryCosts.parseForGermany(clock, ancillaryCosts);
		var helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime = toZonedDateTime(YEAR, 2, 1, 10, 0);
		assertEquals(0.0, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	void testMissingSchedule() throws OpenemsError.OpenemsNamedException {
		var clock = new TimeLeapClock(Instant.parse(YEAR + "-02-01T00:00:00Z"), GERMAN_ZONE_ID);
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", "other") // simulate missing schedule
				.build() //
				.toString();

		var schedule = AncillaryCosts.parseForGermany(clock, ancillaryCosts);
		var helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime = toZonedDateTime(YEAR, 2, 1, 10, 0);
		assertEquals(0.0, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	void testSchedulePrices() throws OpenemsError.OpenemsNamedException {
		var customSchedule = parseToJsonArray(TestData.SCHEDULE);
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", "OTHER") //
				.add("schedule", customSchedule) //
				.build() //
				.toString();

		var clock = new TimeLeapClock(Instant.parse("2026-03-01T00:00:00Z"), ZoneId.systemDefault());
		var schedule = AncillaryCosts.parseForGermany(clock, ancillaryCosts);
		var helper = new TouManualHelper(clock, schedule, 0.0);

		// Validate prices
		var testTime1 = ZonedDateTime.of(LocalDate.of(2026, 3, 1), LocalTime.of(5, 0), ZoneId.systemDefault());
		assertEquals(0.1, helper.getPrices().getAt(testTime1), 0.01);

		var testTime2 = ZonedDateTime.of(LocalDate.of(2026, 3, 1), LocalTime.of(12, 0), ZoneId.systemDefault());
		assertEquals(0.2, helper.getPrices().getAt(testTime2), 0.01);

		var testTime3 = ZonedDateTime.of(LocalDate.of(2026, 3, 1), LocalTime.of(23, 0), ZoneId.systemDefault());
		assertEquals(0.3, helper.getPrices().getAt(testTime3), 0.01);

		// Test Standard tariff if timerange not given.
		clock = new TimeLeapClock(Instant.parse("2026-06-01T00:00:00Z"), ZoneId.systemDefault());
		helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime4 = ZonedDateTime.of(LocalDate.of(2026, 6, 1), LocalTime.of(23, 0), ZoneId.systemDefault());
		assertEquals(0.2, helper.getPrices().getAt(testTime4), 0.01);

		clock = new TimeLeapClock(Instant.parse("2026-07-01T00:00:00Z"), ZoneId.systemDefault());
		helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime5 = ZonedDateTime.of(LocalDate.of(2026, 7, 1), LocalTime.of(10, 0), ZoneId.systemDefault());
		assertEquals(0.2, helper.getPrices().getAt(testTime5), 0.01);

		clock = new TimeLeapClock(Instant.parse("2026-11-01T00:00:00Z"), ZoneId.systemDefault());
		helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime6 = ZonedDateTime.of(LocalDate.of(2026, 11, 1), LocalTime.of(23, 0), ZoneId.systemDefault());
		assertEquals(0.3, helper.getPrices().getAt(testTime6), 0.01);
	}
}
