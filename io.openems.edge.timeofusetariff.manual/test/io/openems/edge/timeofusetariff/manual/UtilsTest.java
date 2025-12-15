package io.openems.edge.timeofusetariff.manual;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.timeofusetariff.api.TouManualHelper.EMPTY_TOU_MANUAL_HELPER;
import static io.openems.edge.timeofusetariff.manual.octopus.Utils.getPrices;
import static io.openems.edge.timeofusetariff.manual.octopus.Utils.getStandardPrice;
import static io.openems.edge.timeofusetariff.manual.octopus.Utils.parseScheduleFromConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.edge.timeofusetariff.api.TouManualHelper;

public class UtilsTest {

	private static final int EUROCENT_CONVERSION = 10;
	private List<String> logMessages;
	private Consumer<String> logWarn;

	private Clock clock;

	@Before
	public void setup() {
		this.clock = createDummyClock();
		this.logMessages = new ArrayList<>();
		this.logWarn = msg -> this.logMessages.add(msg);
	}

	@Test
	public void testGetStandardPriceWithValidDso() throws OpenemsNamedException {
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", "BAYERNWERK") //
				.build() //
				.toString();
		var time = ZonedDateTime.now(this.clock);

		var result = getStandardPrice(ancillaryCosts, time);

		assertTrue("Should return a valid price", result >= 0);
		assertTrue("No warning should be logged", this.logMessages.isEmpty());
	}

	@Test
	public void testGetStandardPriceWithInvalidDsoAndEmptyConfiguration() throws OpenemsNamedException {
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", "INVALID_DSO") //
				.build() //
				.toString();
		var time = ZonedDateTime.now(this.clock);
		var result = getStandardPrice(ancillaryCosts, time);

		assertEquals("Should return 0 for invalid DSO", 0.0, result, 0.001);

		ancillaryCosts = "{}";
		result = getStandardPrice(ancillaryCosts, time);

		assertEquals("Should return 0 for empty configuration", 0.0, result, 0.001);
	}

	@Test
	public void testGetStandardPriceWithCustomSchedule() throws OpenemsNamedException {
		var ancillaryCosts = """
				{
				schedule: [{
					year: 2024,
					tariffs: {
						standard: 8.75
						}
					}]
				}
				""";
		var time = ZonedDateTime.now(this.clock).withYear(2024);
		var result = getStandardPrice(ancillaryCosts, time);

		assertEquals("Should return standard price from schedule", 8.75, result, 0.001);
	}

	@Test
	public void testGetCombinedPricesWithEmptyHelper() {
		final var octopusHelper = createTestOctopusHelper();
		final var ancillaryHelper = EMPTY_TOU_MANUAL_HELPER;
		final var ancillaryConfig = buildJsonObject() //
				.build() //
				.toString();

		var result = getPrices(octopusHelper, ancillaryHelper, ancillaryConfig, this.logWarn);

		assertNotNull("Should return prices even without ancillary helper", result);
		assertTrue("Should return original octopus prices", !result.isEmpty());
	}

	@Test
	public void testGetCombinedPricesWithValidCalculation() throws OpenemsNamedException {
		final var octopusHelper = createTestOctopusHelper();
		final var ancillaryConfig = buildJsonObject() //
				.addProperty("dso", "BAYERNWERK") //
				.build() //
				.toString();

		final var ancillarySchedule = parseScheduleFromConfig(this.clock, ancillaryConfig);
		final var ancillaryHelper = new TouManualHelper(this.clock, ancillarySchedule, 0.0d);

		final var result = getPrices(octopusHelper, ancillaryHelper, ancillaryConfig, this.logWarn);

		assertNotNull("Should return adjusted prices", result);
		assertTrue("Should have adjusted prices", !result.isEmpty());

		final var testTime = ZonedDateTime.now(this.clock).withHour(10).withMinute(0);
		final var octopusPrice = octopusHelper.getPrices().getAt(testTime);
		final var ancillaryActualPrice = ancillaryHelper.getPrices().getAt(testTime) * EUROCENT_CONVERSION;
		final var ancillaryStandardPrice = getStandardPrice(ancillaryConfig, testTime);
		final var expectedPrice = octopusPrice - (ancillaryStandardPrice - ancillaryActualPrice);

		assertEquals("Price should be correctly adjusted", expectedPrice, result.getAt(testTime), 0.001);
	}

	@Test
	public void testGetCombinedPricesWithInvalidConfig() throws OpenemsNamedException {
		final var octopusHelper = createTestOctopusHelper();
		final var invalidAncillaryConfig = "invalid json";

		assertThrows(OpenemsNamedException.class, () -> {
			parseScheduleFromConfig(this.clock, invalidAncillaryConfig);
		});

		final var invalidAncillaryConfig1 = buildJsonObject() //
				.addProperty("invalid", "structure") //
				.build() //
				.toString();

		var ancillarySchedule = parseScheduleFromConfig(this.clock, invalidAncillaryConfig1);
		var ancillaryHelper = new TouManualHelper(this.clock, ancillarySchedule, 0.0d);

		var result = getPrices(octopusHelper, ancillaryHelper, invalidAncillaryConfig, this.logWarn);

		assertNotNull("Should return prices even with configuration error", result);

		final var invalidAncillaryConfig2 = "";
		ancillarySchedule = parseScheduleFromConfig(this.clock, invalidAncillaryConfig2);
		ancillaryHelper = new TouManualHelper(this.clock, ancillarySchedule, 0.0d);

		result = getPrices(octopusHelper, ancillaryHelper, invalidAncillaryConfig, this.logWarn);

		assertNotNull("Should return prices even with configuration error", result);
	}

	// Helper methods
	private static TouManualHelper createTestOctopusHelper() {
		var schedule = JSCalendar.Tasks.<Double>create() //
				.add(t -> t.setStart(LocalTime.of(2, 0)) //
						.setDuration(Duration.ofHours(4)) //
						.addRecurrenceRule(b -> b.setFrequency(//
								JSCalendar.RecurrenceFrequency.DAILY)) //
						.setPayload(12.3) // Low price
						.build())
				.build();
		return new TouManualHelper(//
				createDummyClock(), //
				schedule, 24.8); // Standard price
	}
}
