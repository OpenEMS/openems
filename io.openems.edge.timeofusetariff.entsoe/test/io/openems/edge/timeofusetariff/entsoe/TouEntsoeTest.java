package io.openems.edge.timeofusetariff.entsoe;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.timeofusetariff.api.AncillaryCosts.GridFee;
import io.openems.edge.timeofusetariff.api.GermanDSO;
import io.openems.edge.timeofusetariff.api.TouManualHelper;

public class TouEntsoeTest {
	private static final long FULL_DAY_MINUTES = 1440;

	@Test
	public void test() throws Exception {
		var entsoe = new TouEntsoeImpl();
		var dummyMeta = new DummyMeta("foo0") //
				.withCurrency(EUR);
		new ComponentTest(entsoe) //
				.addReference("meta", dummyMeta) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
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

		var schedule = parseToSchedule(BiddingZone.GERMANY, ancillaryCosts, msg -> fail(msg));
		var clock = new TimeLeapClock(Instant.parse(clockTime), ZoneId.systemDefault());

		return new TouManualHelper(clock, schedule, 0.0);
	}

	@Test
	public void testStandardTariffOnJuly15At2PM() throws OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", "2025-07-15T00:00:00Z");
		var testTime = ZonedDateTime.of(LocalDate.of(2025, 7, 15), LocalTime.of(14, 0), ZoneId.systemDefault());

		assertEquals(8.75, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_ODR", "2025-07-15T00:00:00Z");
		testTime = ZonedDateTime.of(LocalDate.of(2025, 7, 15), LocalTime.of(14, 0), ZoneId.systemDefault());

		assertEquals(3.05, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testLowTariffOnOctober20At3AM() throws OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", "2025-10-20T00:00:00Z");
		var testTime = ZonedDateTime.of(LocalDate.of(2025, 10, 20), LocalTime.of(3, 0), ZoneId.systemDefault());

		assertEquals(0.88, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_BW", "2025-10-20T00:00:00Z");
		testTime = ZonedDateTime.of(LocalDate.of(2025, 10, 20), LocalTime.of(3, 0), ZoneId.systemDefault());

		assertEquals(11.58, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("MIT_NETZ", "2025-10-20T00:00:00Z");
		testTime = ZonedDateTime.of(LocalDate.of(2025, 10, 20), LocalTime.of(3, 0), ZoneId.systemDefault());

		assertEquals(8.95, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("E_DIS", "2025-10-20T00:00:00Z");
		testTime = ZonedDateTime.of(LocalDate.of(2025, 10, 20), LocalTime.of(3, 0), ZoneId.systemDefault());

		assertEquals(0.79, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("LEW", "2025-10-20T00:00:00Z");
		testTime = ZonedDateTime.of(LocalDate.of(2025, 10, 20), LocalTime.of(3, 0), ZoneId.systemDefault());

		assertEquals(6.99, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_ODR", "2025-10-20T00:00:00Z");
		testTime = ZonedDateTime.of(LocalDate.of(2025, 10, 20), LocalTime.of(3, 0), ZoneId.systemDefault());

		assertEquals(7.63, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testHighTariffOnOctober20At5PM() throws OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", "2025-10-20T00:00:00Z");
		var testTime = ZonedDateTime.of(LocalDate.of(2025, 10, 20), LocalTime.of(17, 0), ZoneId.systemDefault());

		assertEquals(11.58, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_BW", "2025-10-20T00:00:00Z");
		testTime = ZonedDateTime.of(LocalDate.of(2025, 10, 20), LocalTime.of(17, 0), ZoneId.systemDefault());

		assertEquals(17.09, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("MIT_NETZ", "2025-10-20T00:00:00Z");
		testTime = ZonedDateTime.of(LocalDate.of(2025, 10, 20), LocalTime.of(17, 0), ZoneId.systemDefault());

		assertEquals(17.9, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("WEST_NETZ", "2025-10-20T00:00:00Z");
		testTime = ZonedDateTime.of(LocalDate.of(2025, 10, 20), LocalTime.of(17, 0), ZoneId.systemDefault());

		assertEquals(17.75, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("AVACON", "2025-10-20T00:00:00Z");
		testTime = ZonedDateTime.of(LocalDate.of(2025, 10, 20), LocalTime.of(17, 0), ZoneId.systemDefault());

		assertEquals(15.01, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_ODR", "2025-10-20T00:00:00Z");
		testTime = ZonedDateTime.of(LocalDate.of(2025, 10, 20), LocalTime.of(17, 0), ZoneId.systemDefault());

		assertEquals(7.63, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testStandardTariffOnApril10At12PM() throws OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", "2025-04-10T00:00:00Z");
		var testTime = ZonedDateTime.of(LocalDate.of(2025, 4, 10), LocalTime.of(12, 0), ZoneId.systemDefault());

		assertEquals(8.75, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_BW", "2025-04-10T00:00:00Z");
		testTime = ZonedDateTime.of(LocalDate.of(2025, 4, 10), LocalTime.of(12, 0), ZoneId.systemDefault());
		assertEquals(4.63, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_BW", "2025-04-09T00:00:00Z");
		testTime = ZonedDateTime.of(LocalDate.of(2025, 4, 10), LocalTime.of(0, 0), ZoneId.systemDefault());

		assertEquals(11.58, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_ODR", "2025-04-09T00:00:00Z");
		testTime = ZonedDateTime.of(LocalDate.of(2025, 4, 10), LocalTime.of(0, 0), ZoneId.systemDefault());

		assertEquals(13.23, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testEmptyScheduleReturnsZeroPrice() throws OpenemsNamedException {
		var clock = new TimeLeapClock(Instant.parse("2025-02-01T00:00:00Z"), ZoneId.systemDefault());
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", "other") //
				.add("schedule", buildJsonArray() //
						.build()) // empty
				.build() //
				.toString();

		var schedule = parseToSchedule(BiddingZone.GERMANY, ancillaryCosts, msg -> fail(msg));
		var helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime = ZonedDateTime.of(LocalDate.of(2025, 2, 1), LocalTime.of(10, 0), ZoneId.systemDefault());
		assertEquals(0.0, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testMissingSchedule() throws OpenemsNamedException {
		var clock = new TimeLeapClock(Instant.parse("2025-02-01T00:00:00Z"), ZoneId.systemDefault());
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", "other") // simulate missing schedule
				.build() //
				.toString();

		var schedule = parseToSchedule(BiddingZone.GERMANY, ancillaryCosts, msg -> fail(msg));
		var helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime = ZonedDateTime.of(LocalDate.of(2025, 2, 1), LocalTime.of(10, 0), ZoneId.systemDefault());
		assertEquals(0.0, helper.getPrices().getAt(testTime), 0.01);
	}
}
