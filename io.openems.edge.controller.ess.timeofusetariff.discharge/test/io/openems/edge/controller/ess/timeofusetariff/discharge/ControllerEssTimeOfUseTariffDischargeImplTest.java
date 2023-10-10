package io.openems.edge.controller.ess.timeofusetariff.discharge;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

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

public class ControllerEssTimeOfUseTariffDischargeImplTest {

	// Ids
	private static final String CTRL_ID = "ctrlEssTimeOfUseTariffDischarge0";
	private static final String PREDICTOR_ID = "predictor0";
	private static final String ESS_ID = "ess0";

	// Ess channels
	private static final ChannelAddress ESS_CAPACITY = new ChannelAddress(ESS_ID, "Capacity");
	private static final ChannelAddress ESS_SOC = new ChannelAddress(ESS_ID, "Soc");

	// Controller Channels
	private static final ChannelAddress TOTAL_CONSUMPTION = new ChannelAddress(CTRL_ID, "TotalConsumption");
	private static final ChannelAddress REMAINING_CONSUMPTION = new ChannelAddress(CTRL_ID, "RemainingConsumption");
	private static final ChannelAddress AVAILABLE_CAPACITY = new ChannelAddress(CTRL_ID, "AvailableCapacity");
	private static final ChannelAddress USABLE_CAPACITY = new ChannelAddress(CTRL_ID, "UsableCapacity");
	private static final ChannelAddress QUATERLY_PRICES_TAKEN = new ChannelAddress(CTRL_ID, "QuaterlyPricesTaken");
	private static final ChannelAddress TARGET_HOURS_CALCULATED = new ChannelAddress(CTRL_ID, "TargetHoursCalculated");
	private static final ChannelAddress TARGET_HOURS_IS_EMPTY = new ChannelAddress(CTRL_ID, "TargetHoursIsEmpty");
	private static final ChannelAddress TARGET_HOURS = new ChannelAddress(CTRL_ID, "TargetHours");
	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(CTRL_ID, "StateMachine");

	/*
	 * Default Prediction values
	 */
	private static final Integer[] DEFAULT_PRODUCTION_PREDICTION = {
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

	private static final Integer[] DEFAULT_CONSUMPTION_PREDICTION = {

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

	@Test
	public void nullTimeOfUseTariffTest() throws Exception {

		final var clock = new TimeLeapClock(Instant.parse("2021-01-01T13:45:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictions
		final var productionPrediction = new DummyPrediction24Hours(DEFAULT_PRODUCTION_PREDICTION);
		final var consumptionPrediction = new DummyPrediction24Hours(DEFAULT_CONSUMPTION_PREDICTION);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, consumptionPrediction,
				"_sum/UnmanagedConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		// Price provider
		final var timeOfUseTariffProvider = DummyTimeOfUseTariffProvider.fromHourlyPrices(null, DEFAULT_HOURLY_PRICES);

		// Printing
		// System.out.println("Time: " + clock);
		// System.out.println(Arrays.toString(predictorManager
		// .get24HoursPrediction(ChannelAddress.fromString("_sum/ProductionActivePower")).getValues()));
		// System.out.println(Arrays.toString(predictorManager
		// .get24HoursPrediction(ChannelAddress.fromString("_sum/ConsumptionActivePower")).getValues()));

		new ControllerTest(new ControllerEssTimeOfUseTariffDischargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addReference("timeOfUseTariff", timeOfUseTariffProvider) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMaxStartHour(8) //
						.setMaxEndHour(16) //
						.setMode(Mode.AUTOMATIC) //
						.setDelayDischargeRiskLevel(DelayDischargeRiskLevel.HIGH) //
						.build())
				.next(new TestCase("Cycle - 1") //
						.output(AVAILABLE_CAPACITY, null) //
						.output(QUATERLY_PRICES_TAKEN, false) //
						.output(TARGET_HOURS_CALCULATED, false) //
						.output(TARGET_HOURS_IS_EMPTY, true) //
						.output(STATE_MACHINE, StateMachine.STANDBY));
	}

	@Test
	public void executesDuringMarketTimeTest() throws Exception {

		final var clock = new TimeLeapClock(Instant.parse("2021-01-01T16:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictions
		final var productionPrediction = new DummyPrediction24Hours(DEFAULT_PRODUCTION_PREDICTION);
		final var consumptionPrediction = new DummyPrediction24Hours(DEFAULT_CONSUMPTION_PREDICTION);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, consumptionPrediction,
				"_sum/UnmanagedConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		// Price provider
		final var timeOfUseTariffProvider = DummyTimeOfUseTariffProvider.fromHourlyPrices(ZonedDateTime.now(clock),
				DEFAULT_HOURLY_PRICES);

		// Printing
		// System.out.println("Time: " + clock);
		// System.out.println(Arrays.toString(predictorManager
		// .get24HoursPrediction(ChannelAddress.fromString("_sum/ProductionActivePower")).getValues()));
		// System.out.println(Arrays.toString(predictorManager
		// .get24HoursPrediction(ChannelAddress.fromString("_sum/ConsumptionActivePower")).getValues()));

		new ControllerTest(new ControllerEssTimeOfUseTariffDischargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addReference("timeOfUseTariff", timeOfUseTariffProvider) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMaxStartHour(8) //
						.setMaxEndHour(16) //
						.setMode(Mode.AUTOMATIC) //
						.setDelayDischargeRiskLevel(DelayDischargeRiskLevel.HIGH) //
						.build())
				.next(new TestCase("Cycle - 1") //
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(ESS_CAPACITY, 12000) //
						.input(ESS_SOC, 100) //
						.output(AVAILABLE_CAPACITY, 12000) //
						.output(QUATERLY_PRICES_TAKEN, true) //
						.output(TARGET_HOURS_CALCULATED, true)//
						.output(TARGET_HOURS_IS_EMPTY, false)//
						.output(TOTAL_CONSUMPTION, 15248) //
						.output(REMAINING_CONSUMPTION, 3248.0) //
						.output(STATE_MACHINE, StateMachine.ALLOWS_DISCHARGE))
				.next(new TestCase("Cycle - 2") //
						.output(QUATERLY_PRICES_TAKEN, false) //
						.output(TARGET_HOURS_CALCULATED, false)//
						.output(TARGET_HOURS_IS_EMPTY, false) //
						.output(STATE_MACHINE, StateMachine.ALLOWS_DISCHARGE))
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMaxStartHour(8) //
						.setMaxEndHour(16) //
						.setMode(Mode.OFF) //
						.setDelayDischargeRiskLevel(DelayDischargeRiskLevel.HIGH) //
						.build())
				.next(new TestCase("Cycle - 3") //
						.output(QUATERLY_PRICES_TAKEN, false) //
						.output(TARGET_HOURS_CALCULATED, false)//
						.output(TARGET_HOURS_IS_EMPTY, true) //
						.output(STATE_MACHINE, StateMachine.STANDBY))
				.next(new TestCase("Cycle - 4") //
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.output(QUATERLY_PRICES_TAKEN, false) //
						.output(TARGET_HOURS_CALCULATED, false)//
						.output(TARGET_HOURS_IS_EMPTY, true) //
						.output(STATE_MACHINE, StateMachine.STANDBY));
	}

	@Test
	public void executesBeforeMidnight() throws Exception {

		final var clock = new TimeLeapClock(Instant.parse("2021-01-01T21:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictions
		final var productionPrediction = new DummyPrediction24Hours(DEFAULT_PRODUCTION_PREDICTION);
		final var consumptionPrediction = new DummyPrediction24Hours(DEFAULT_CONSUMPTION_PREDICTION);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, consumptionPrediction,
				"_sum/UnmanagedConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		// Price provider
		final var timeOfUseTariffProvider = DummyTimeOfUseTariffProvider.fromHourlyPrices(ZonedDateTime.now(clock),
				DEFAULT_HOURLY_PRICES);

		// Printing
		// System.out.println("Time: " + clock);
		// System.out.println(Arrays.toString(predictorManager
		// .get24HoursPrediction(ChannelAddress.fromString("_sum/ProductionActivePower")).getValues()));
		// System.out.println(Arrays.toString(predictorManager
		// .get24HoursPrediction(ChannelAddress.fromString("_sum/UnmanagedConsumptionActivePower")).getValues()));

		new ControllerTest(new ControllerEssTimeOfUseTariffDischargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addReference("timeOfUseTariff", timeOfUseTariffProvider) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMaxStartHour(8) //
						.setMaxEndHour(16) //
						.setMode(Mode.AUTOMATIC) //
						.setDelayDischargeRiskLevel(DelayDischargeRiskLevel.HIGH) //
						.build())
				.next(new TestCase("Cycle - 1") //
						.input(ESS_CAPACITY, 12000) //
						.input(ESS_SOC, 100) //
						.output(AVAILABLE_CAPACITY, 12000) //
						.output(USABLE_CAPACITY, 12000) //
						.output(REMAINING_CONSUMPTION, 0.0) //
						.output(QUATERLY_PRICES_TAKEN, true) //
						.output(TARGET_HOURS_CALCULATED, false) //
						.output(TARGET_HOURS_IS_EMPTY, true) //
						.output(STATE_MACHINE, StateMachine.ALLOWS_DISCHARGE));
	}

	@Test
	public void executesAfterMidnight() throws Exception {

		final var clock = new TimeLeapClock(Instant.parse("2021-01-01T11:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictions
		final var productionPrediction = new DummyPrediction24Hours(DEFAULT_PRODUCTION_PREDICTION);
		final var consumptionPrediction = new DummyPrediction24Hours(DEFAULT_CONSUMPTION_PREDICTION);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, consumptionPrediction,
				"_sum/UnmanagedConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		// Price provider
		final var timeOfUseTariffProvider = DummyTimeOfUseTariffProvider.fromHourlyPrices(ZonedDateTime.now(clock),
				DEFAULT_HOURLY_PRICES);

		// Printing
		// System.out.println("Time: " + clock);
		// System.out.println(Arrays.toString(predictorManager
		// .get24HoursPrediction(ChannelAddress.fromString("_sum/ProductionActivePower")).getValues()));
		// System.out.println(Arrays.toString(predictorManager
		// .get24HoursPrediction(ChannelAddress.fromString("_sum/UnmanagedConsumptionActivePower")).getValues()));

		new ControllerTest(new ControllerEssTimeOfUseTariffDischargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addReference("timeOfUseTariff", timeOfUseTariffProvider) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMaxStartHour(8) //
						.setMaxEndHour(16) //
						.setMode(Mode.AUTOMATIC) //
						.setDelayDischargeRiskLevel(DelayDischargeRiskLevel.HIGH) //
						.build())
				.next(new TestCase("Cycle - 1") //
						.output(AVAILABLE_CAPACITY, null) //
						.output(USABLE_CAPACITY, null) //
						.output(TARGET_HOURS, null) //
						.output(QUATERLY_PRICES_TAKEN, true) //
						.output(TARGET_HOURS_CALCULATED, false) //
						.output(TARGET_HOURS_IS_EMPTY, true) //
						.output(STATE_MACHINE, StateMachine.STANDBY));
	}
}
