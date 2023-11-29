package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.TestData.CONSUMPTION_PREDICTION_QUARTERLY;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.HOURLY_PRICES_SUMMER;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRODUCTION_PREDICTION_QUARTERLY;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.STATES;
import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.getNowRoundedDownToMinutes;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.junit.Test;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.ScheduleData;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class ScheduleTest {

	@Test
	public void createScheduleTest() {
		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var timestamp = getNowRoundedDownToMinutes(ZonedDateTime.now(clock), 15).minusHours(3);

		// Price provider
		final var quarterlyPrices = DummyTimeOfUseTariffProvider
				.quarterlyPrices(ZonedDateTime.now(clock), HOURLY_PRICES_SUMMER).getPrices().getValues();

		var datas = new ArrayList<ScheduleData>();
		var size = quarterlyPrices.length;
		for (var i = 0; i < size; i++) {
			final var price = new JsonPrimitive(quarterlyPrices[i]);
			final var state = new JsonPrimitive(STATES[i]);
			final var production = new JsonPrimitive(PRODUCTION_PREDICTION_QUARTERLY[i]);
			final var consumption = new JsonPrimitive(CONSUMPTION_PREDICTION_QUARTERLY[i]);

			datas.add(new ScheduleData(price, state, production, consumption, JsonNull.INSTANCE));
		}

		final var result = Utils.createSchedule(datas, timestamp);

		// Check if the consumption power is converted to energy.
		final var expectedProductionResult = CONSUMPTION_PREDICTION_QUARTERLY[0].intValue();
		assertEquals(expectedProductionResult, result.get(0).getAsJsonObject().get("consumption").getAsInt());

		// Check if the result is same size as prices.
		assertEquals(size, result.size());

		var expectedLastTimestamp = timestamp.plusDays(1).minusMinutes(15).format(DateTimeFormatter.ISO_INSTANT)
				.toString();
		var generatedLastTimestamp = result.get(95).getAsJsonObject().get("timestamp").getAsString();

		// Check if the last timestamp is as expected.
		assertEquals(expectedLastTimestamp, generatedLastTimestamp);
	}
}
