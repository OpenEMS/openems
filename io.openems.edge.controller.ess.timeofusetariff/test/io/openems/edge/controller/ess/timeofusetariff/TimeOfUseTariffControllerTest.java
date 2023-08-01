package io.openems.edge.controller.ess.timeofusetariff;

import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.predictor.api.test.DummyPrediction24Hours;
import io.openems.edge.predictor.api.test.DummyPredictor24Hours;
import io.openems.edge.predictor.api.test.DummyPredictorManager;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class TimeOfUseTariffControllerTest {

	// Ids
	private static final String CTRL_ID = "ctrlEssTimeOfUseTariff0";
	private static final String PREDICTOR_ID = "predictor0";
	private static final String ESS_ID = "ess0";

	// Ess channels
	private static final ChannelAddress ESS_CAPACITY = new ChannelAddress(ESS_ID, "Capacity");
	private static final ChannelAddress MAX_APPARENT_POWER = new ChannelAddress(ESS_ID, "MaxApparentPower");
	private static final ChannelAddress ESS_SOC = new ChannelAddress(ESS_ID, "Soc");

	/*
	 * Default Prediction values
	 */
	private static final Integer[] DEFAULT_PRODUCTION_PREDICTION_QUARTERLY = {
			/* 00:00-03:450 */
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
			/* 04:00-07:45 */
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 74, 297, 610, //
			/* 08:00-11:45 */
			913, 1399, 1838, 2261, 2662, 3052, 3405, 3708, 4011, 4270, 4458, 4630, 4794, 4908, 4963, 4960, //
			/* 12:00-15:45 */
			4973, 4940, 4859, 4807, 4698, 4530, 4348, 4147, 1296, 1399, 1838, 1261, 1662, 1052, 1405, 1402,
			/* 16:00-19:45 */
			1662, 1052, 1405, 1630, 1285, 1520, 1250, 910, 0, 0, 0, 0, 0, 0, 0, 0, //
			/* 20:00-23:45 */
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
			/* 00:00-03:45 */
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
			/* 04:00-07:45 */
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 130, 402, 667, //
			/* 08:00-11:45 */
			1023, 1631, 2020, 2420, 2834, 3237, 3638, 4006, 4338, 4597, 4825, 4965, 5111, 5213, 5268, 5317, //
			/* 12:00-15:45 */
			5321, 5271, 5232, 5193, 5044, 4915, 4738, 4499, 3702, 3226, 3046, 2857, 2649, 2421, 2184, 1933, //
			/* 16:00-19:45 */
			1674, 1364, 1070, 754, 447, 193, 52, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
			/* 20:00-23:45 */
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 //
	};

	private static final Integer[] DEFAULT_PRODUCTION_PREDICTION_HOURLY = {
			/* 00:00-12:00 */
			0, 0, 0, 0, 0, 0, 0, 74, 297, 610, 913, 1399,
			/* 13:00-24:00 */
			1838, 2261, 2662, 3052, 1520, 1250, 910, 0, 0, 0, 0, 0, //
	};

	private static final Integer[] DEFAULT_CONSUMPTION_PREDICTION_HOURLY = {
			/* 00:00-12:00 */
			1021, 1208, 713, 931, 2847, 2551, 1558, 1234, 433, 633, 1355, 606, //
			/* 13:00-24:00 */
			430, 1432, 1121, 502, 294, 1048, 1194, 914, 1534, 1226, 1235, 977, //
	};

	private static final Integer[] DEFAULT_CONSUMPTION_PREDICTION_QUARTERLY = {

			/* 00:00-03:450 */
			1021, 1208, 713, 931, 2847, 2551, 1558, 1234, 433, 633, 1355, 606, 430, 1432, 1121, 502, //
			/* 04:00-07:45 */
			294, 1048, 1194, 914, 1534, 1226, 1235, 977, 578, 1253, 1983, 1417, 513, 929, 1102, 445, //
			/* 08:00-11:45 */
			1208, 2791, 2729, 2609, 2086, 1454, 848, 816, 2610, 3150, 2036, 1180, 359, 1316, 3447, 2104, //
			/* 12:00-15:45 */
			905, 802, 828, 812, 863, 633, 293, 379, 1250, 2296, 2436, 2140, 2135, 1196, 2230, 1725,
			/* 16:00-19:45 */
			2365, 1758, 2325, 2264, 2181, 2167, 2228, 1082, 777, 417, 798, 1268, 409, 830, 1191, 417, //
			/* 20:00-23:45 */
			1087, 2958, 2946, 2235, 1343, 483, 796, 1201, 567, 395, 989, 1066, 370, 989, 1255, 660, //
			/* 00:00-03:45 */
			349, 880, 1186, 580, 327, 911, 1135, 553, 265, 938, 1165, 567, 278, 863, 1239, 658, //
			/* 04:00-07:45 */
			236, 816, 1173, 1131, 498, 550, 1344, 1226, 874, 504, 1733, 1809, 1576, 369, 771, 2583, //
			/* 08:00-11:45 */
			3202, 2174, 1878, 2132, 2109, 1895, 1565, 1477, 1613, 1716, 1867, 1726, 1700, 1787, 1755, 1734, //
			/* 12:00-15:45 */
			1380, 691, 338, 168, 199, 448, 662, 205, 183, 70, 169, 276, 149, 76, 195, 168, //
			/* 16:00-19:45 */
			159, 266, 135, 120, 224, 979, 2965, 1337, 1116, 795, 334, 390, 433, 369, 762, 2908, //
			/* 20:00-23:45 */
			3226, 2358, 1778, 1002, 455, 654, 534, 1587, 1638, 459, 330, 258, 368, 728, 1096, 878 //
	};

	private static final Float[] DEFAULT_HOURLY_PRICES = { 158.95f, 160.98f, 171.95f, 174.96f, //
			161.93f, 152f, 120.01f, 111.03f, //
			105.04f, 105f, 74.23f, 73.28f, //
			67.97f, 72.53f, 89.66f, 150.01f, //
			173.54f, 178.4f, 158.91f, 140.01f, //
			149.99f, 157.43f, 130.9f, 120.14f //
	};

	private static final Float[] DEFAULT_HOURLY_PRICES_SUMMER = { 70.95f, 71.98f, 71.95f, 74.96f, //
			78.93f, 80f, 84.01f, 111.03f, //
			105.04f, 105f, 74.23f, 73.28f, //
			67.97f, 72.53f, 89.66f, 150.01f, //
			173.54f, 178.4f, 158.91f, 140.01f, //
			149.99f, 157.43f, 130.9f, 120.14f //
	};

	// Predictions
	final DummyPrediction24Hours productionPredictionQuarterly = new DummyPrediction24Hours(
			DEFAULT_PRODUCTION_PREDICTION_QUARTERLY);
	final DummyPrediction24Hours consumptionPredictionQuarterly = new DummyPrediction24Hours(
			DEFAULT_CONSUMPTION_PREDICTION_QUARTERLY);
	final DummyPrediction24Hours productionPredictionHourly = new DummyPrediction24Hours(
			DEFAULT_PRODUCTION_PREDICTION_HOURLY);
	final DummyPrediction24Hours consumptionPredictionHourly = new DummyPrediction24Hours(
			DEFAULT_CONSUMPTION_PREDICTION_HOURLY);

	@Test
	public void scheduleChargeForEveryQuarter() throws Exception {

		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, this.productionPredictionQuarterly,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm,
				this.consumptionPredictionQuarterly, "_sum/ConsumptionActivePower");

		// Predictor Manager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		// Price provider
		final var timeOfUseTariffProvider = DummyTimeOfUseTariffProvider.quarterlyPrices(ZonedDateTime.now(clock),
				DEFAULT_HOURLY_PRICES);

		new ControllerTest(new TimeOfUseTariffControllerImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addReference("timeOfUseTariff", timeOfUseTariffProvider) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMode(Mode.AUTOMATIC) //
						.setControlMode(ControlMode.CHARGE_CONSUMPTION) //
						.setMaxPower(4000) //
						.build())
				.next(new TestCase("Cycle - 1") //
						.input(MAX_APPARENT_POWER, 9000) //
						.input(ESS_CAPACITY, 12000) //
						.input(ESS_SOC, 100) //
				);
	}

	@Test
	public void scheduleChargeForEveryHour() throws Exception {

		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, this.productionPredictionHourly,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, this.consumptionPredictionHourly,
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		// Price provider
		final var timeOfUseTariffProvider = DummyTimeOfUseTariffProvider.hour1yPrices(ZonedDateTime.now(clock),
				DEFAULT_HOURLY_PRICES_SUMMER);

		new ControllerTest(new TimeOfUseTariffControllerImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addReference("timeOfUseTariff", timeOfUseTariffProvider) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMode(Mode.AUTOMATIC) //
						.setControlMode(ControlMode.CHARGE_CONSUMPTION) //
						.setMaxPower(6000) //
						.build())
				.next(new TestCase("Cycle - 1") //
						.input(MAX_APPARENT_POWER, 9000) //
						.input(ESS_CAPACITY, 12000) //
						.input(ESS_SOC, 50) //
				);
	}

	@Test
	public void scheduleDelayDischargeForEveryHour() throws Exception {

		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, this.productionPredictionHourly,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, this.consumptionPredictionHourly,
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		// Price provider
		final var timeOfUseTariffProvider = DummyTimeOfUseTariffProvider.hour1yPrices(ZonedDateTime.now(clock),
				DEFAULT_HOURLY_PRICES_SUMMER);

		new ControllerTest(new TimeOfUseTariffControllerImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addReference("timeOfUseTariff", timeOfUseTariffProvider) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMode(Mode.AUTOMATIC) //
						.setControlMode(ControlMode.DELAY_DISCHARGE) //
						.build())
				.next(new TestCase("Cycle - 1") //
						.input(MAX_APPARENT_POWER, 9000) //
						.input(ESS_CAPACITY, 12000) //
						.input(ESS_SOC, 50) //
				);
	}

	@Test
	public void scheduleDelayDischargeForEveryQuarter() throws Exception {

		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, this.productionPredictionQuarterly,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm,
				this.consumptionPredictionQuarterly, "_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		// Price provider
		final var timeOfUseTariffProvider = DummyTimeOfUseTariffProvider.quarterlyPrices(ZonedDateTime.now(clock),
				DEFAULT_HOURLY_PRICES);

		new ControllerTest(new TimeOfUseTariffControllerImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addReference("timeOfUseTariff", timeOfUseTariffProvider) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMode(Mode.AUTOMATIC) //
						.setControlMode(ControlMode.DELAY_DISCHARGE).build())
				.next(new TestCase("Cycle - 1") //
						.input(MAX_APPARENT_POWER, 9000) //
						.input(ESS_CAPACITY, 12000) //
						.input(ESS_SOC, 50) //
				);
	}

	@Test
	public void scheduleTest() {

		final var essUsableEnergy = 12000;
		final var currentAvailableEnergy = 6000;
		final var dischargeEnergy = 2250;
		final var chargeEnergy = -2250;
		var allowedChargeEnergyFromGrid = 0;

		var schedule = new Schedule(ControlMode.DELAY_DISCHARGE, //
				essUsableEnergy, //
				currentAvailableEnergy, //
				dischargeEnergy, //
				chargeEnergy, //
				DEFAULT_HOURLY_PRICES_SUMMER, //
				DEFAULT_CONSUMPTION_PREDICTION_HOURLY, //
				DEFAULT_PRODUCTION_PREDICTION_HOURLY, //
				allowedChargeEnergyFromGrid);

		schedule.createSchedule();

		var expectedBatteryValues = Arrays.asList(0, 0, 0, 0, //
				873, 2250, 1558, 1160, //
				136, 23, 0, -793, //
				-1408, -829, -1541, -2250, //
				-1226, -202, 284, 914, //
				1534, 1226, 1235, 977);

		var calculatedBatteryValues = schedule.periods.stream().map(t -> {
			return t.chargeDischargeEnergy;
		}).collect(Collectors.toList());

		assertTrue(expectedBatteryValues.equals(calculatedBatteryValues));

		allowedChargeEnergyFromGrid = 1500;

		schedule = new Schedule(ControlMode.CHARGE_CONSUMPTION, //
				essUsableEnergy, //
				currentAvailableEnergy, //
				dischargeEnergy, //
				chargeEnergy, //
				DEFAULT_HOURLY_PRICES_SUMMER, //
				DEFAULT_CONSUMPTION_PREDICTION_HOURLY, //
				DEFAULT_PRODUCTION_PREDICTION_HOURLY, //
				allowedChargeEnergyFromGrid);

		schedule.createSchedule();

		expectedBatteryValues = Arrays.asList(-479, -292, -787, -569, //
				2250, 2250, 1558, 1160, //
				136, 23, 442, -793, //
				-1408, -829, -1541, -2250, //
				-1226, -202, 284, 914, //
				1534, 1226, 1235, 977);

		calculatedBatteryValues = schedule.periods.stream().map(t -> {
			return t.chargeDischargeEnergy;
		}).collect(Collectors.toList());

		assertTrue(expectedBatteryValues.equals(calculatedBatteryValues));
	}
}
