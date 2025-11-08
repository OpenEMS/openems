package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.parseToJsonArray;
import static io.openems.edge.common.currency.Currency.EUR;
import static io.openems.edge.timeofusetariff.entsoe.Utils.parseToSchedule;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.timeofusetariff.api.TouManualHelper;

public class TouEntsoeAncillaryCostsOtherTest {

	private static String SCHEDULE = """
			[
			  {
			    "year": 2025,
			    "tariffs": {
			      "low": 0.10,
			      "standard": 0.20,
			      "high": 0.30
			    },
			    "quarters": [
			      {
			        "quarter": 1,
			        "dailySchedule": [
			          { "tariff": "low", "from": "00:00", "to": "06:00" },
			          { "tariff": "standard", "from": "06:00", "to": "18:00" },
			          { "tariff": "high", "from": "18:00", "to": "23:59" }
			        ]
			      },
				  {
			        "quarter": 2,
			        "dailySchedule": [ ]
			      },
				  {
			        "quarter": 3,
			        "dailySchedule": [ ]
			      },
				  {
			        "quarter": 4,
			        "dailySchedule": [
			          { "tariff": "low", "from": "00:00", "to": "06:00" },
			          { "tariff": "standard", "from": "06:00", "to": "18:00" },
			          { "tariff": "high", "from": "18:00", "to": "23:59" }
			        ]
			      }
			    ]
			  }
			]
			""";

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		var entsoe = new TouEntsoeImpl();
		var dummyMeta = new DummyMeta("foo0") //
				.withCurrency(EUR);
		var schedule = parseToJsonArray(SCHEDULE);
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
								.addProperty("dso", "OTHER") //
								.add("schedule", schedule) //
								.build() //
								.toString())
						.build());
	}

	@Test
	public void testPrices() throws OpenemsNamedException {
		var customSchedule = parseToJsonArray(SCHEDULE);
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", "OTHER") //
				.add("schedule", customSchedule) //
				.build() //
				.toString();

		var clock = new TimeLeapClock(Instant.parse("2025-03-01T00:00:00Z"), ZoneId.systemDefault());
		var schedule = parseToSchedule(BiddingZone.GERMANY, ancillaryCosts, System.out::println);
		var helper = new TouManualHelper(clock, schedule, 0.0);

		// Validate prices
		var testTime1 = ZonedDateTime.of(LocalDate.of(2025, 3, 1), LocalTime.of(5, 0), ZoneId.systemDefault());
		assertEquals(0.1, helper.getPrices().getAt(testTime1), 0.01);

		var testTime2 = ZonedDateTime.of(LocalDate.of(2025, 3, 1), LocalTime.of(12, 0), ZoneId.systemDefault());
		assertEquals(0.2, helper.getPrices().getAt(testTime2), 0.01);

		var testTime3 = ZonedDateTime.of(LocalDate.of(2025, 3, 1), LocalTime.of(23, 0), ZoneId.systemDefault());
		assertEquals(0.3, helper.getPrices().getAt(testTime3), 0.01);

		// Test Standard tariff if timerange not given.
		clock = new TimeLeapClock(Instant.parse("2025-06-01T00:00:00Z"), ZoneId.systemDefault());
		helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime4 = ZonedDateTime.of(LocalDate.of(2025, 6, 1), LocalTime.of(23, 0), ZoneId.systemDefault());
		assertEquals(0.2, helper.getPrices().getAt(testTime4), 0.01);

		clock = new TimeLeapClock(Instant.parse("2025-07-01T00:00:00Z"), ZoneId.systemDefault());
		helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime5 = ZonedDateTime.of(LocalDate.of(2025, 7, 1), LocalTime.of(10, 0), ZoneId.systemDefault());
		assertEquals(0.2, helper.getPrices().getAt(testTime5), 0.01);

		clock = new TimeLeapClock(Instant.parse("2025-11-01T00:00:00Z"), ZoneId.systemDefault());
		helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime6 = ZonedDateTime.of(LocalDate.of(2025, 11, 1), LocalTime.of(23, 0), ZoneId.systemDefault());
		assertEquals(0.3, helper.getPrices().getAt(testTime6), 0.01);
	}

}
