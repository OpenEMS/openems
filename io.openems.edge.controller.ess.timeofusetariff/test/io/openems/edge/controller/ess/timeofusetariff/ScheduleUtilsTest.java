package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.TestData.CONSUMPTION_PREDICTION_QUARTERLY;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.HOURLY_PRICES;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PAST_HOURLY_PRICES;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRODUCTION_PREDICTION_QUARTERLY;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PAST_STATES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class ScheduleUtilsTest {

	@Test
	public void testHandleGetScheduleRequest() {
		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T00:00:00.00Z"), ZoneOffset.UTC);

		final var timestamp = TimeOfUseTariffUtils.getNowRoundedDownToMinutes(ZonedDateTime.now(), 15).minusHours(3);
		final var channeladdressPrices = new ChannelAddress("", "QuarterlyPrices");
		final var channeladdressStateMachine = new ChannelAddress("", "StateMachine");

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> dummyQueryResult = new TreeMap<>();

		for (int i = 0; i < 12; i++) {
			SortedMap<ChannelAddress, JsonElement> dummyChannelValues = new TreeMap<>();
			dummyChannelValues.put(channeladdressPrices, new JsonPrimitive(PAST_HOURLY_PRICES[i]));
			dummyChannelValues.put(channeladdressStateMachine, new JsonPrimitive(PAST_STATES[i]));

			dummyQueryResult.put(timestamp.plusMinutes(i * 15), dummyChannelValues);
		}

		// Price provider
		final var timeOfUseTariffProvider = DummyTimeOfUseTariffProvider.quarterlyPrices(ZonedDateTime.now(clock),
				HOURLY_PRICES);
		final var controlMode = ControlMode.CHARGE_CONSUMPTION;
		final var schedule = Schedule.createSchedule(controlMode, RiskLevel.HIGH, 12000, 12000, 2250, -2250,
				timeOfUseTariffProvider.getPrices().getValues(), CONSUMPTION_PREDICTION_QUARTERLY,
				PRODUCTION_PREDICTION_QUARTERLY, 1000);

		final var result = ScheduleUtils.handleGetScheduleRequest(schedule, controlMode, null, dummyQueryResult,
				channeladdressPrices, channeladdressStateMachine);

		JsonUtils.prettyPrint(result.getResult());

		final var scheduleArray = result.getResult().get("schedule").getAsJsonArray();

		// Check if the logic generates 96 values.
		assertEquals(96, scheduleArray.size());

		// Check if first value of last three hour data present in schedule.
		assertTrue(scheduleArray.get(0).getAsJsonObject().get("price").getAsDouble() == 158.95f);

		// Check if last value of 96 hourly prices array is avoided, since the logic
		// limits to 96 values.
		assertTrue(scheduleArray.get(95).getAsJsonObject().get("price").getAsDouble() != 120.14f);
	}

}
