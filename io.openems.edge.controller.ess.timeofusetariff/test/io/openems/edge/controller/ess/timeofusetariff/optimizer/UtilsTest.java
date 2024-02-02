package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.edge.common.test.TestUtils.withValue;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.CONSUMPTION_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.CONSUMPTION_PREDICTION_QUARTERLY;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.HOURLY_PRICES_SUMMER;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PAST_HOURLY_PRICES;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PAST_SOC;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PAST_STATES;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRICES_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRODUCTION_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRODUCTION_PREDICTION_QUARTERLY;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.STATES;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest.getComponentManager;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest.getOptimizer;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest.getPredictorManager;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest.getTimeOfUseTariff;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.SimulatorTest.TIME;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.SimulatorTest.createParams888d20231106;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.SimulatorTest.hourlyToQuarterly;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.ESS_MAX_SOC;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.SUM_CONSUMPTION;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.SUM_ESS_SOC;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.SUM_PRODUCTION;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.buildInitialPopulation;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateBalancingChargeDischarge;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateCharge;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateEssMaxCharge;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateEssMaxDischarge;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateExecutionLimitSeconds;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateStateChargeEnergy;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.createSchedule;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.essMaxChargePower;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.generateProductionPrediction;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.handleGetScheduleRequest;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.interpolateArray;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.joinConsumptionPredictions;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.paramsAreValid;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.postprocessRunState;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.postprocessSimulatorState;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.toEnergy;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.toPower;
import static java.lang.Integer.MIN_VALUE;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.stream;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImplTest;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.ScheduleData;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyHybridEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.test.DummyPredictor;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class UtilsTest {

	@Test
	public void testInterpolateArrayFloat() {
		assertArrayEquals(new double[] { 123, 123, 234, 234, 345 }, //
				interpolateArray(new Double[] { null, 123., 234., null, 345., null }), //
				0.0001F);

		assertArrayEquals(new double[] {}, //
				interpolateArray(new Double[] { null }), //
				0.0001F);
	}

	@Test
	public void testInterpolateArrayInteger() {
		assertArrayEquals(new int[] { 123, 123, 234, 234, 345 }, //
				interpolateArray(new Integer[] { null, 123, 234, null, 345, null }));

		assertArrayEquals(new int[] {}, //
				interpolateArray(new Integer[] { null }));

		assertArrayEquals(new int[] { 123, 123 }, //
				interpolateArray(new Integer[] { null, 123 }));

		assertArrayEquals(new int[] { 123 }, //
				interpolateArray(new Integer[] { 123, null }));
	}

	@Test
	public void testToPower() {
		assertEquals(2000, (int) toPower(500));
		assertNull(toPower(null));
	}

	@Test
	public void testGenerateProductionPrediction() {
		final var arr = new Integer[] { 1, 2, 3 };
		assertArrayEquals(arr, generateProductionPrediction(arr, 2));
		assertArrayEquals(new Integer[] { 1, 2, 3, 0 }, generateProductionPrediction(arr, 4));
	}

	@Test
	public void testJoinConsumptionPredictions() {
		assertArrayEquals(//
				new Integer[] { 1, 2, 3, 4, 55, 66, 77, 88, 99 }, //
				joinConsumptionPredictions(4, //
						new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, //
						new Integer[] { 11, 22, 33, 44, 55, 66, 77, 88, 99 }));
	}

	@Test
	public void testCalculateEssMaxCharge() {
		assertEquals(0, calculateEssMaxCharge(0, 0, 0));
		assertEquals(-400, calculateEssMaxCharge(1000, 400, 0));
		assertEquals(-300, calculateEssMaxCharge(1000, 400, 700));
		assertEquals(0, calculateEssMaxCharge(1000, 400, 1100));
		assertEquals(-400, calculateEssMaxCharge(1000, 400, -100));
	}

	@Test
	public void testCalculateEssMaxDischarge() {
		assertEquals(0, calculateEssMaxDischarge(0, 0, 0));
		assertEquals(400, calculateEssMaxDischarge(100, 400, 1000));
		assertEquals(300, calculateEssMaxDischarge(100, 400, 400));
		assertEquals(0, calculateEssMaxDischarge(100, 400, 0));
	}

	@Test
	public void testCalculateBalancingChargeDischarge() {
		assertEquals(0, calculateBalancingChargeDischarge(0, 0, 0, 0));
		assertEquals(-200, calculateBalancingChargeDischarge(-1000, 1000, 500, 300));
		assertEquals(200, calculateBalancingChargeDischarge(-1000, 1000, 500, 700));
		assertEquals(1000, calculateBalancingChargeDischarge(-1000, 1000, 5000, 7000));
		assertEquals(-1000, calculateBalancingChargeDischarge(-1000, 1000, 5000, 3000));
	}

	@Test
	public void testCalculateStateChargeEnergy() {
		assertEquals(0, calculateStateChargeEnergy(0, 0, 0, 0, 0));

		assertEquals(-1234, calculateStateChargeEnergy(//
				2000 /* ess charge limit */, 24_000 /* grid limit */, 1000 /* consumption */, 2000 /* production */,
				-1234));

		assertEquals(-1100, calculateStateChargeEnergy(//
				1100 /* ess charge limit */, 24_000 /* grid limit */, 100 /* consumption */, 200 /* production */,
				MIN_VALUE /* no limit */));

		// 'maxBuyFromGrid' is already completely consumed by predicted consumption
		// (minus production). => simulate charge with 1 Wh to trigger optimizer to
		// consider CHARGE instead of BALANCING mode
		assertEquals(-1, calculateStateChargeEnergy(//
				5000 /* ess charge limit */, 5000 /* grid limit */, 20000 /* consumption */, 0 /* production */,
				-2500));
	}

	@Test
	public void testCalculateCharge() {
		assertEquals(-2500, calculateCharge(//
				new DummyManagedSymmetricEss("ess0") //
						.withActivePower(-1000), //
				new DummySum() //
						.withGridActivePower(500), //
				/* essMaxChargePower */ 2_500, //
				/* maxChargePowerFromGrid */ 24_000).intValue());

		// Would be 5000, but can never be positive
		assertEquals(0, calculateCharge(//
				new DummyManagedSymmetricEss("ess0") //
						.withActivePower(1000), //
				new DummySum() //
						.withGridActivePower(9000), //
				/* essMaxChargePower */ 5_000, //
				/* maxChargePowerFromGrid */ 5_000).intValue());

		assertEquals(-2000, calculateCharge(//
				new DummyHybridEss("ess0") //
						.withActivePower(-1000) //
						.withDcDischargePower(-1500), //
				new DummySum() //
						.withGridActivePower(-2000), //
				/* essMaxChargePower */ 2_500, //
				/* maxChargePowerFromGrid */ 24_000).intValue());
	}

	@Test
	public void testParamsAreValid() throws Exception {
		var builder = Params.create() //
				.time(TIME) //
				.essInitialEnergy(0) //
				.essTotalEnergy(22000) //
				.essMinSocEnergy(2_000) //
				.essMaxSocEnergy(20_000) //
				.maxBuyFromGrid(toEnergy(24_000)) //
				.maxBuyFromGrid(0) //
				.states(new StateMachine[0]);

		// No periods are available
		assertFalse(paramsAreValid(builder //
				.productions() //
				.consumptions() //
				.prices() //
				.build()));

		// Production and Consumption predictions are all zero
		assertFalse(paramsAreValid(builder //
				.productions(0, 0, 0) //
				.consumptions(0, 0) //
				.prices(123F) //
				.build()));

		// Prices are all the same
		assertFalse(paramsAreValid(builder //
				.productions(0, 1, 3) //
				.consumptions(0, 2) //
				.prices(123F, 123F) //
				.build()));

		// Finally got it right...
		assertTrue(paramsAreValid(builder //
				.productions(0, 1, 3) //
				.consumptions(0, 2) //
				.prices(123F, 124F) //
				.build()));
		assertEquals(2, builder.build().numberOfPeriods());
	}

	@Test
	public void testHandleScheduleRequest() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-03-04T14:19:00.00Z"), ZoneOffset.UTC);
		final var ctrl = TimeOfUseTariffControllerImplTest.create(clock);
		final var predictorManager = getPredictorManager(ctrl);
		final var componentManager = getComponentManager(ctrl);
		final var timeOfUseTariff = getTimeOfUseTariff(ctrl);

		// Simulate historic data
		var now = roundDownToQuarter(ZonedDateTime.now(clock));
		final var fromDate = now.minusHours(3);
		var timedata = new DummyTimedata("timedata0");
		for (var i = 0; i < 12; i++) {
			var quarter = fromDate.plusMinutes(i * 15);
			timedata.add(quarter, new ChannelAddress("", "QuarterlyPrices"), PAST_HOURLY_PRICES[i]);
			timedata.add(quarter, new ChannelAddress("", "StateMachine"), PAST_STATES[i]);
			timedata.add(quarter, SUM_PRODUCTION, PRODUCTION_PREDICTION_QUARTERLY[i]);
			timedata.add(quarter, SUM_CONSUMPTION, CONSUMPTION_PREDICTION_QUARTERLY[i]);
			timedata.add(quarter, SUM_ESS_SOC, PAST_SOC[i]);
		}

		final var midnight = now.truncatedTo(DAYS);
		predictorManager.addPredictor(new DummyPredictor("predictor0", componentManager,
				Prediction.from(SUM_PRODUCTION, midnight, PRODUCTION_PREDICTION_QUARTERLY), SUM_PRODUCTION));
		predictorManager.addPredictor(new DummyPredictor("predictor0", componentManager,
				Prediction.from(SUM_CONSUMPTION, midnight, CONSUMPTION_PREDICTION_QUARTERLY), SUM_CONSUMPTION));
		timeOfUseTariff.setPrices(TimeOfUsePrices.from(now, 1., 1.));

		var optimizer = getOptimizer(ctrl);
		optimizer.forever();

		// Testing only past data. For full data, optimizer has to be created as well.
		var result = handleGetScheduleRequest(optimizer, randomUUID(), timedata, "", clock.now()).getResult();

		// JsonUtils.prettyPrint(result);

		var schedule = getAsJsonArray(result, "schedule");
		assertEquals(14, schedule.size());
		{
			var period = getAsJsonObject(schedule.get(0));
			assertEquals(PAST_HOURLY_PRICES[0], getAsFloat(period, "price"), 0.00F);
			assertEquals(PRODUCTION_PREDICTION_QUARTERLY[0] / 4, getAsInt(period, "production"));
		}
		{
			var period = getAsJsonObject(schedule.get(12));
			assertEquals(BALANCING.getValue(), getAsInt(period, "state"));
			assertEquals(60, getAsInt(period, "soc"));
		}
		{
			var period = getAsJsonObject(schedule.get(13));
			assertEquals(BALANCING.getValue(), getAsInt(period, "state"));
			assertEquals(58, getAsInt(period, "soc"));
		}
	}

	@Test
	public void testPostprocessPeriodState() {
		var p = Params.create() //
				.time(TIME) //
				.essInitialEnergy(0) //
				.essTotalEnergy(22000) //
				.essMinSocEnergy(2_000) //
				.essMaxSocEnergy(20_000) //
				.essMaxEnergyPerPeriod(0) //
				.maxBuyFromGrid(toEnergy(24_000)) //
				.productions() //
				.consumptions() //
				.prices(new double[] { 123 }) //
				.states(new StateMachine[0]) //
				.build();

		assertEquals("BALANCING stays BALANCING", //
				BALANCING, postprocessSimulatorState(p, 0, 0, 0, 0, 0, BALANCING));

		assertEquals("DELAY_DISCHARGE but battery is empty", //
				BALANCING, postprocessSimulatorState(p, 0, 2000, 0, 0, 0, DELAY_DISCHARGE));
		assertEquals("DELAY_DISCHARGE and would discharge in balancing", //
				DELAY_DISCHARGE, postprocessSimulatorState(p, 0, 2001, 0, 0, 1, DELAY_DISCHARGE));
		assertEquals("DELAY_DISCHARGE and would charge from PV in balancing", //
				BALANCING, postprocessSimulatorState(p, -1000, 2001, 0, 0, -1000, DELAY_DISCHARGE));
		assertEquals("DELAY_DISCHARGE but price is the max price", //
				BALANCING, postprocessSimulatorState(p, -1000, 2001, 1, 123F, 0, DELAY_DISCHARGE));
		assertEquals("DELAY_DISCHARGE and price is NOT the max price", //
				DELAY_DISCHARGE, postprocessSimulatorState(p, -1000, 2001, -1, 122.9F, 0, DELAY_DISCHARGE));

		assertEquals("CHARGE actually from grid", //
				CHARGE, postprocessSimulatorState(p, -1000, 0, 1, 0, 0, CHARGE));
		assertEquals("CHARGE but fully supplied by excess PV", //
				BALANCING, postprocessSimulatorState(p, -1000, 0, 0, 0, 0, CHARGE));
		assertEquals("CHARGE but battery is full", //
				DELAY_DISCHARGE, postprocessSimulatorState(p, -1000, 20_001, 1, 0, 0, CHARGE));
		assertEquals("CHARGE but price is close to max", //
				DELAY_DISCHARGE, postprocessSimulatorState(p, 0, 0, 1, 102.51F, 0, CHARGE));
		assertEquals("CHARGE and price is NOT close to max", //
				CHARGE, postprocessSimulatorState(p, 0, 0, 1, 102.50F, 0, CHARGE));
	}

	@Test
	public void testPostprocessRunState() {
		// SoC undefined -> all stay
		assertEquals(BALANCING, postprocessRunState(0, null, BALANCING));
		assertEquals(DELAY_DISCHARGE, postprocessRunState(0, null, DELAY_DISCHARGE));
		assertEquals(CHARGE, postprocessRunState(0, null, CHARGE));

		assertEquals("BALANCING stays BALANCING", //
				BALANCING, postprocessRunState(10, 10, DELAY_DISCHARGE));

		assertEquals("DELAY_DISCHARGE but SoC is at Min-SoC", //
				BALANCING, postprocessRunState(10, 10, DELAY_DISCHARGE));
		assertEquals("DELAY_DISCHARGE and SoC is above Min-SoC", //
				DELAY_DISCHARGE, postprocessRunState(10, 11, DELAY_DISCHARGE));

		assertEquals("CHARGE but SoC is at Max-SoC", //
				DELAY_DISCHARGE, postprocessRunState((int) ESS_MAX_SOC, (int) ESS_MAX_SOC + 1, CHARGE));
		assertEquals("CHARGE and SoC is below or equal Max-SoC", //
				CHARGE, postprocessRunState((int) ESS_MAX_SOC, (int) ESS_MAX_SOC, CHARGE));
	}

	@Test
	public void testBuildInitialPopulation() {
		{
			var gt = buildInitialPopulation(Params.create() //
					.productions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.consumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.prices(hourlyToQuarterly(interpolateArray(PRICES_888_20231106))) //
					.states(ControlMode.CHARGE_CONSUMPTION.states) //
					.existingSchedule(CHARGE, DELAY_DISCHARGE, CHARGE, DELAY_DISCHARGE, BALANCING) //
					.build()).get(0);
			assertEquals(2 /* CHARGE */, gt.get(0).get(0).intValue());
			assertEquals(1 /* DELAY_DISCHARGE */, gt.get(1).get(0).intValue());
			assertEquals(2 /* CHARGE */, gt.get(2).get(0).intValue());
			assertEquals(1 /* DELAY_DISCHARGE */, gt.get(3).get(0).intValue());
			assertEquals(0 /* BALANCING */, gt.get(4).get(0).intValue());
			assertEquals(0 /* BALANCING */, gt.get(5).get(0).intValue()); // default
		}
		{
			var gt = buildInitialPopulation(Params.create() //
					.productions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.consumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
					.prices(hourlyToQuarterly(interpolateArray(PRICES_888_20231106))) //
					.states(ControlMode.DELAY_DISCHARGE.states) //
					.existingSchedule(CHARGE, DELAY_DISCHARGE, CHARGE, DELAY_DISCHARGE, BALANCING) //
					.build()).get(0);
			assertEquals(0 /* fallback to BALANCING */, gt.get(0).get(0).intValue());
			assertEquals(1 /* DELAY_DISCHARGE */, gt.get(1).get(0).intValue());
			assertEquals(0 /* fallback to BALANCING */, gt.get(2).get(0).intValue());
			assertEquals(1 /* DELAY_DISCHARGE */, gt.get(3).get(0).intValue());
			assertEquals(0 /* BALANCING */, gt.get(4).get(0).intValue());
			assertEquals(0 /* BALANCING */, gt.get(5).get(0).intValue()); // default
		}
	}

	@Test
	public void testCreateSchedule() {
		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var timestamp = roundDownToQuarter(ZonedDateTime.now(clock)).minusHours(3);

		// Price provider
		final var quarterlyPrices = DummyTimeOfUseTariffProvider.fromHourlyPrices(clock, HOURLY_PRICES_SUMMER)
				.getPrices().asArray();

		var datas = new ArrayList<ScheduleData>();
		var size = quarterlyPrices.length;
		for (var i = 0; i < size; i++) {
			datas.add(new ScheduleData(//
					quarterlyPrices[i], //
					STATES[i], //
					PRODUCTION_PREDICTION_QUARTERLY[i], //
					CONSUMPTION_PREDICTION_QUARTERLY[i], //
					null));
		}

		final var result = createSchedule(datas, timestamp);

		// Check if the consumption power is converted to energy.
		assertEquals(//
				(int) toPower(CONSUMPTION_PREDICTION_QUARTERLY[0]), //
				result.get(0).getAsJsonObject().get("consumption").getAsInt());

		// Check if the result is same size as prices.
		assertEquals(size, result.size());

		var expectedLastTimestamp = timestamp.plusDays(1).minusMinutes(15).format(DateTimeFormatter.ISO_INSTANT)
				.toString();
		var generatedLastTimestamp = result.get(95).getAsJsonObject().get("timestamp").getAsString();

		// Check if the last timestamp is as expected.
		assertEquals(expectedLastTimestamp, generatedLastTimestamp);
	}

	@Test
	public void testCalculateExecutionLimitSeconds() {
		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		assertEquals(Duration.ofMinutes(14).plusSeconds(30).toSeconds(), calculateExecutionLimitSeconds(clock));

		clock.leap(11, ChronoUnit.MINUTES);
		assertEquals(Duration.ofMinutes(3).plusSeconds(30).toSeconds(), calculateExecutionLimitSeconds(clock));

		clock.leap(150, ChronoUnit.SECONDS);
		assertEquals(60, calculateExecutionLimitSeconds(clock));

		clock.leap(1, ChronoUnit.SECONDS);
		assertEquals(Duration.ofMinutes(15).plusSeconds(59).toSeconds(), calculateExecutionLimitSeconds(clock));
	}

	@Test
	public void testEssMaxChargePower() {
		final var params = createParams888d20231106(ControlMode.CHARGE_CONSUMPTION.states);
		final var ess = new DummyManagedSymmetricEss("ess0");

		// No params, initial ESS
		assertEquals(0, essMaxChargePower(null, ess));

		// No params, ESS with MaxApparentPower
		withValue(ess, SymmetricEss.ChannelId.MAX_APPARENT_POWER, 1000);
		assertEquals(250, essMaxChargePower(null, ess));

		// No params, ESS with Capacity
		withValue(ess, SymmetricEss.ChannelId.CAPACITY, 15000);
		assertEquals(3750, essMaxChargePower(null, ess));

		// With params (22 kWh)
		assertEquals(5500, essMaxChargePower(params, ess));
	}
}
