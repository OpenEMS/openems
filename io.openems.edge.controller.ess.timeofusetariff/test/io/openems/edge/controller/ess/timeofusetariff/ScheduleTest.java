package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.TestData.CONSUMPTION_PREDICTION_HOURLY;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.HOURLY_PRICES_SUMMER;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRODUCTION_PREDICTION_HOURLY;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.STATES;
import static org.junit.Assert.assertArrayEquals;
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
	public void scheduleTest() {
		final var essUsableEnergy = 12000;
		final var essMaxDischargePower = 9000;
		final var essMaxChargePower = -9000;

		var essInitialEnergy = 3000;

		var schedule = Schedule.createSchedule(ControlMode.DELAY_DISCHARGE, //
				RiskLevel.HIGH, //
				essUsableEnergy, //
				essInitialEnergy, //
				essMaxDischargePower, //
				essMaxChargePower, //
				HOURLY_PRICES_SUMMER, //
				CONSUMPTION_PREDICTION_HOURLY, //
				PRODUCTION_PREDICTION_HOURLY, //
				0 /* maxChargePowerFromGrid */);

		var expectedBatteryValues = new Integer[] { 111, 302, 178, 232, //
				711, 637, 389, 290, //
				34, 6, 110, -198, //
				-352, -207, -385, -638, //
				-307, -50, 71, 228, //
				383, 306, 308, 244 };

		var calculatedBatteryValues = schedule.periods.stream() //
				.map(t -> t.chargeDischargeEnergy) //
				.toArray();

		assertArrayEquals(expectedBatteryValues, calculatedBatteryValues);

		// Adjusting the current energy.
		essInitialEnergy = 800;

		schedule = Schedule.createSchedule(ControlMode.CHARGE_CONSUMPTION, //
				RiskLevel.HIGH, //
				essUsableEnergy, //
				essInitialEnergy, //
				essMaxDischargePower, //
				essMaxChargePower, //
				HOURLY_PRICES_SUMMER, //
				CONSUMPTION_PREDICTION_HOURLY, //
				PRODUCTION_PREDICTION_HOURLY, //
				3000 /* maxChargePowerFromGrid */);

		expectedBatteryValues = new Integer[] { -495, -448, -572, 232, //
				711, 637, 389, 290, //
				34, 6, 16, -198, //
				-352, -207, -385, -638, //
				-307, -50, 71, 228, //
				383, 306, 308, 244 };

		calculatedBatteryValues = schedule.periods.stream() //
				.map(t -> t.chargeDischargeEnergy) //
				.toArray();

		assertArrayEquals(expectedBatteryValues, calculatedBatteryValues);
	}

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
