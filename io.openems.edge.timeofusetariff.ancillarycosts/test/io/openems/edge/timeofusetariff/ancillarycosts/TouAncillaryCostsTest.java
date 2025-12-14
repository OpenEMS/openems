package io.openems.edge.timeofusetariff.ancillarycosts;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.common.currency.Currency.EUR;
import static org.junit.Assert.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.timeofusetariff.api.GermanDSO;

public class TouAncillaryCostsTest {

	private static final String COMPONENT_ID = "tou0";

	@Test
	public void testBasicActivation() throws Exception {
		var ac = new TouAncillaryCostsImpl();
		var clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00.00Z"), ZoneId.of("Europe/Berlin"));

		new ComponentTest(ac) //
				.addReference("meta", new DummyMeta("foo").withCurrency(EUR)) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setFixedTariff(0.0) //
						.setAncillaryCosts(buildJsonObject() //
								.addProperty("dso", "BAYERNWERK") //
								.build() //
								.toString()) //
						.build());

		// In July, Bayernwerk has a single standard tariff of 8.75
		var bayernwerkPrice = GermanDSO.BAYERNWERK.getPriceAt(ZonedDateTime.now(clock)) * 10;
		assertEquals(bayernwerkPrice, ac.getPrices().getFirst(), 0.01);
	}

	@Test
	public void testBayernWerkWithFixedTariff() throws Exception {
		final double fixedPrice = 1.0;
		final var ac = new TouAncillaryCostsImpl();

		var summerClock = Clock.fixed(Instant.parse("2026-01-01T10:30:00.00Z"), ZoneId.of("Europe/Berlin"));

		new ComponentTest(ac) //
				.addReference("meta", new DummyMeta("foo") //
						.withCurrency(EUR)) //
				.addReference("componentManager", new DummyComponentManager(summerClock)) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setFixedTariff(fixedPrice) //
						.setAncillaryCosts(buildJsonObject() //
								.addProperty("dso", "BAYERNWERK") //
								.build() //
								.toString()) //
						.build());

		var summerAncillaryPrice = GermanDSO.BAYERNWERK.getPriceAt(ZonedDateTime.now(summerClock));
		var summerPrices = ac.getPrices();
		var expectedPrice = (summerAncillaryPrice + fixedPrice) * 10;
		assertEquals(expectedPrice, summerPrices.getFirst(), 0.01);

		var autumnClock = Clock.fixed(Instant.parse("2025-10-01T18:00:00.00Z"), ZoneId.of("Europe/Berlin"));

		new ComponentTest(ac) //
				.addReference("meta", new DummyMeta("foo") //
						.withCurrency(EUR)) //
				.addReference("componentManager", new DummyComponentManager(autumnClock)) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setFixedTariff(fixedPrice) //
						.setAncillaryCosts(buildJsonObject() //
								.addProperty("dso", "BAYERNWERK") //
								.build() //
								.toString()) //
						.build());

		var autumnAncillaryPrice = GermanDSO.BAYERNWERK.getPriceAt(ZonedDateTime.now(autumnClock));
		var autumnPrices = ac.getPrices();
		expectedPrice = (autumnAncillaryPrice + fixedPrice) * 10;
		assertEquals(expectedPrice, autumnPrices.getFirst(), 0.01);
	}

	@Test
	public void testWithCustomSchedule() throws Exception {
		final var fixedTariff = 10.0;
		final var customScheduleJson = """
				{
				  "dso": "OTHER",
				  "schedule": [
				    {
				      "year": 2025,
				      "tariffs": { "low": 5.0, "standard": 15.0, "high": 25.0 },
				      "quarters": [
				        {
				          "quarter": 3,
				          "dailySchedule": [
				            { "from": "08:00", "to": "18:00", "tariff": "HIGH" },
				            { "from": "00:00", "to": "06:00", "tariff": "LOW" }
				          ]
				        }
				      ]
				    }
				  ]
				}
				""";

		var ac = new TouAncillaryCostsImpl();
		var clock = Clock.fixed(Instant.parse("2025-07-29T00:00:00.00Z"), ZoneId.of("Europe/Berlin"));

		new ComponentTest(ac) //
				.addReference("meta", new DummyMeta("foo") //
						.withCurrency(EUR)) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setFixedTariff(fixedTariff) //
						.setAncillaryCosts(customScheduleJson) //
						.build());

		var prices = ac.getPrices();

		// At 08:30, expect HIGH tariff (25.0) + fixed tariff (10.0)
		var highTime = ZonedDateTime.now(clock).withHour(8).withMinute(30);
		var expectedPrice = (25.0 + fixedTariff) * 10;
		assertEquals(expectedPrice, prices.getAt(highTime), 0.01);

		// At 02:00, expect LOW tariff (5.0) + fixed tariff (10.0)
		var lowTime = ZonedDateTime.now(clock).withHour(2).withMinute(0);
		expectedPrice = (5.0 + fixedTariff) * 10;
		assertEquals(expectedPrice, prices.getAt(lowTime), 0.01);

		// At 07:00 (a gap), expect STANDARD tariff (15.0) + fixed tariff (10.0)
		var standardTime = ZonedDateTime.now(clock).withHour(7).withMinute(0);
		expectedPrice = (15.0 + fixedTariff) * 10;
		assertEquals(expectedPrice, prices.getAt(standardTime), 0.01);
	}

	@Test
	public void testWithInvalidAncillaryCosts() throws Exception {
		var ac = new TouAncillaryCostsImpl();
		var test = new ComponentTest(ac) //
				.addReference("meta", new DummyMeta("foo") //
						.withCurrency(EUR))
				.addReference("componentManager", new DummyComponentManager(createDummyClock())); //

		test.activate(MyConfig.create() //
				.setId(COMPONENT_ID) //
				.setFixedTariff(20.0) //
				.setAncillaryCosts("{}") //
				.build());

		// Since it's an invalid json, the default price 0 will be taken.
		var expectedPrice = (20.0 + 0) * 10;
		assertEquals(expectedPrice, ac.getPrices().getFirst(), 0.01);
	}
}