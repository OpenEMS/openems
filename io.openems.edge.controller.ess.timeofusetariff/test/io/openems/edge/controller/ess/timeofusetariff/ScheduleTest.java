package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.TestData.HOURLY_PRICES_SUMMER;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.STATES;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

import com.google.gson.JsonArray;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class ScheduleTest {

	@Test
	public void createScheduleTest() {
		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var timestamp = TimeOfUseTariffUtils.getNowRoundedDownToMinutes(ZonedDateTime.now(clock), 15)
				.minusHours(3);

		var states = new JsonArray();
		var prices = new JsonArray();

		// Price provider
		final var timeOfUseTariffProvider = DummyTimeOfUseTariffProvider.quarterlyPrices(ZonedDateTime.now(clock),
				HOURLY_PRICES_SUMMER);

		for (Float price : timeOfUseTariffProvider.getPrices().getValues()) {
			prices.add(price);
		}

		for (Integer state : STATES) {
			states.add(state);
		}

		final var result = ScheduleUtils.createSchedule(prices, states, timestamp);

		// Check if the result is same size as prices.
		assertEquals(prices.size(), result.size());

		var expectedLastTimestamp = timestamp.plusDays(1).minusMinutes(15).format(DateTimeFormatter.ISO_INSTANT)
				.toString();
		var generatedLastTimestamp = result.get(95).getAsJsonObject().get("timestamp").getAsString();

		// Check if the last timestamp is as expected.
		assertEquals(expectedLastTimestamp, generatedLastTimestamp);
	}
}
