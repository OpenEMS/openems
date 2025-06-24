package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.common.currency.Currency.EUR;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.timeofusetariff.api.TouManualHelper;

public class TouEntsoeTest {

	private static final Double STANDARD_TARIFF = 8.75;
	private static final Double LOW_TARIFF = 0.88;
	private static final Double HIGH_TARIFF = 11.58;

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

	private TouManualHelper buildHelper(String dso, String clockTime) throws OpenemsNamedException {
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", dso) //
				.build().toString();

		var schedule = AncillaryCosts.parseToSchedule(BiddingZone.GERMANY, ancillaryCosts,
				msg -> System.out.println(msg));
		var clock = new TimeLeapClock(Instant.parse(clockTime), ZoneId.systemDefault());

		return new TouManualHelper(clock, schedule, 0.0);
	}

	@Test
	public void testStandardTariffOnJuly15At14PM() throws OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", "2025-07-15T00:00:00Z");
		var testTime = ZonedDateTime.of(LocalDate.of(2025, 7, 15), LocalTime.of(14, 0), ZoneId.systemDefault());
		assertEquals(STANDARD_TARIFF, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testLowTariffOnOctober20At3AM() throws OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", "2025-10-20T00:00:00Z");
		var testTime = ZonedDateTime.of(LocalDate.of(2025, 10, 20), LocalTime.of(3, 0), ZoneId.systemDefault());

		assertEquals(LOW_TARIFF, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testHighTariffOnOctober20At5PM() throws OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", "2025-10-20T00:00:00Z");
		var testTime = ZonedDateTime.of(LocalDate.of(2025, 10, 20), LocalTime.of(17, 0), ZoneId.systemDefault());

		assertEquals(HIGH_TARIFF, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testStandardTariffOnApril10At12PM() throws OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", "2025-04-10T00:00:00Z");
		var testTime = ZonedDateTime.of(LocalDate.of(2025, 4, 10), LocalTime.of(12, 0), ZoneId.systemDefault());
		assertEquals(STANDARD_TARIFF, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testEmptyScheduleReturnsZeroPrice() throws OpenemsNamedException {
		var clock = new TimeLeapClock(Instant.parse("2025-02-01T00:00:00Z"), ZoneId.systemDefault());
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", "other") // simulate missing schedule
				.add("schedule", buildJsonArray().build()) // empty
				.build() //
				.toString();

		var schedule = AncillaryCosts.parseToSchedule(BiddingZone.GERMANY, ancillaryCosts,
				msg -> System.out.println(msg));
		var helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime = ZonedDateTime.of(LocalDate.of(2025, 2, 1), LocalTime.of(10, 0), ZoneId.systemDefault());
		assertEquals(0.0, helper.getPrices().getAt(testTime), 0.01);
	}
}
