package io.openems.edge.controller.ess.gridoptimizedcharge;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;

import org.junit.Test;

import io.openems.common.function.ThrowingRunnable;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyHybridEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.meter.test.DummyAsymmetricMeter;
import io.openems.edge.predictor.api.test.DummyPrediction24Hours;
import io.openems.edge.predictor.api.test.DummyPredictor24Hours;
import io.openems.edge.predictor.api.test.DummyPredictorManager;

public class MyControllerTest {

	// Ids
	private static final String CTRL_ID = "ctrlGridOptimizedCharge0";
	private static final String PREDICTOR_ID = "predictor0";
	private static final String ESS_ID = "ess0";
	private static final String METER_ID = "meter0";

	// Components
	private static final DummyManagedSymmetricEss ESS = new DummyManagedSymmetricEss(ESS_ID);
	private static final DummyAsymmetricMeter METER = new DummyAsymmetricMeter(METER_ID);
	private static final DummyHybridEss HYBRID_ESS = new DummyHybridEss(ESS_ID);
	private static final DummyManagedSymmetricEss ESS_WITH_NONE_APPARENT_POWER = new DummyManagedSymmetricEss(ESS_ID,
			new DummyPower(0));

	// Ess channels
	private static final ChannelAddress ESS_CAPACITY = new ChannelAddress(ESS_ID, "Capacity");
	private static final ChannelAddress ESS_SOC = new ChannelAddress(ESS_ID, "Soc");
	private static final ChannelAddress ESS_MAX_APPARENT_POWER = new ChannelAddress(ESS_ID, "MaxApparentPower");
	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "ActivePower");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerLessOrEquals");

	// Meter channels
	private static final ChannelAddress METER_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");

	// Controller channels
	private static final ChannelAddress PREDICTED_TARGET_MINUTE = new ChannelAddress(CTRL_ID, "PredictedTargetMinute");
	private static final ChannelAddress PREDICTED_TARGET_MINUTE_ADJUSTED = new ChannelAddress(CTRL_ID,
			"PredictedTargetMinuteAdjusted");
	private static final ChannelAddress TARGET_MINUTE = new ChannelAddress(CTRL_ID, "TargetMinute");
	private static final ChannelAddress DELAY_CHARGE_STATE = new ChannelAddress(CTRL_ID, "DelayChargeState");
	private static final ChannelAddress SELL_TO_GRID_LIMIT_STATE = new ChannelAddress(CTRL_ID, "SellToGridLimitState");
	private static final ChannelAddress DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT = new ChannelAddress(CTRL_ID,
			"DelayChargeMaximumChargeLimit");
	private static final ChannelAddress RAW_DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT = new ChannelAddress(CTRL_ID,
			"RawDelayChargeMaximumChargeLimit");
	private static final ChannelAddress SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT = new ChannelAddress(CTRL_ID,
			"SellToGridLimitMinimumChargeLimit");
	private static final ChannelAddress RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT = new ChannelAddress(CTRL_ID,
			"RawSellToGridLimitChargeLimit");
	private static final ChannelAddress START_EPOCH_SECONDS = new ChannelAddress(CTRL_ID, "StartEpochSeconds");

	// Sum channels
	private static final ChannelAddress SUM_PRODUCTION_DC_ACTUAL_POWER = new ChannelAddress("_sum",
			"ProductionDcActualPower");
	private static final ChannelAddress SUM_PRODUCTION_ACTIVE_POWER = new ChannelAddress("_sum",
			"ProductionActivePower");
	private static final ChannelAddress SUM_CONSUMPTION_ACTIVE_POWER = new ChannelAddress("_sum",
			"ConsumptionActivePower");

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
			4973, 4940, 4859, 4807, 4698, 4530, 4348, 4147, 3527, 3141, 2917, 2703, 2484, 2233, 1971, 1674, //
			/* 16:00-19:45 */
			1386, 1089, 811, 531, 298, 143, 41, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
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
			905, 802, 828, 812, 863, 633, 293, 379, 296, 296, 436, 140, 135, 196, 230, 175, //
			/* 16:00-19:45 */
			365, 758, 325, 264, 181, 167, 228, 1082, 777, 417, 798, 1268, 409, 830, 1191, 417, //
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

	@Test
	public void automatic_default_predictions_at_midnight_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictions
		final var productionPrediction = new DummyPrediction24Hours(DEFAULT_PRODUCTION_PREDICTION);
		final var consumptionPrediction = new DummyPrediction24Hours(DEFAULT_CONSUMPTION_PREDICTION);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, consumptionPrediction,
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ProductionActivePower")).getValues()));
		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ConsumptionActivePower")).getValues()));

		new ControllerTest(new GridOptimizedChargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS) //
				.addReference("meter", METER) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMaximumSellToGridPower(7_000) //
						.setMeterId(METER_ID) //
						.setDelayChargeRiskLevel(DelayChargeRiskLevel.MEDIUM) //
						.setMode(Mode.AUTOMATIC) //
						.setSellToGridLimitEnabled(true) //
						.setSellToGridLimitRampPercentage(5) //
						.setManualTargetTime("") //
						.build()) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, 0) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(ESS_CAPACITY, 10_000) //
						.input(ESS_SOC, 20) //
						.input(ESS_MAX_APPARENT_POWER, 10_000) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(PREDICTED_TARGET_MINUTE, /* QuarterHour */ 68 * 15) //
						.output(PREDICTED_TARGET_MINUTE_ADJUSTED, /* QuarterHour */ 68 * 15 - 120) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.ACTIVE_LIMIT) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, -6650) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, 6650) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 540));
	}

	@Test
	public void automatic_default_predictions_at_midday_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T12:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictions
		final var productionPrediction = new DummyPrediction24Hours(DEFAULT_PRODUCTION_PREDICTION);
		final var consumptionPrediction = new DummyPrediction24Hours(DEFAULT_CONSUMPTION_PREDICTION);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, consumptionPrediction,
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ProductionActivePower")).getValues()));
		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ConsumptionActivePower")).getValues()));

		new ControllerTest(new GridOptimizedChargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS) //
				.addReference("meter", METER) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMaximumSellToGridPower(7_000) //
						.setMeterId(METER_ID) //
						.setDelayChargeRiskLevel(DelayChargeRiskLevel.MEDIUM) //
						.setMode(Mode.AUTOMATIC) //
						.setSellToGridLimitEnabled(true) //
						.setSellToGridLimitRampPercentage(5) //
						.setManualTargetTime("") //
						.build()) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, 0) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(ESS_CAPACITY, 10_000) //
						.input(ESS_SOC, 20) //
						.input(ESS_MAX_APPARENT_POWER, 10_000) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(PREDICTED_TARGET_MINUTE, /* QuarterHour */ 68 * 15) //
						.output(PREDICTED_TARGET_MINUTE_ADJUSTED, /* QuarterHour */ 68 * 15 - 120) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.ACTIVE_LIMIT) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, -6650) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, 6650) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2700));
	}

	@Test
	public void automatic_default_predictions_at_midday_averaged_test() throws Exception {
		// Sleep between every TestCase to make sure that the Channel Values are added
		// to the pastValues Map. This is required because the Channel Value timestamp
		// does not consider the mocked Clock.
		final ThrowingRunnable<Exception> sleep = () -> Thread.sleep(10);

		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T12:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictions
		final var productionPrediction = new DummyPrediction24Hours(DEFAULT_PRODUCTION_PREDICTION);
		final var consumptionPrediction = new DummyPrediction24Hours(DEFAULT_CONSUMPTION_PREDICTION);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, consumptionPrediction,
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ProductionActivePower")).getValues()));
		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ConsumptionActivePower")).getValues()));

		new ControllerTest(new GridOptimizedChargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS) //
				.addReference("meter", METER) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMaximumSellToGridPower(7_000) //
						.setMeterId(METER_ID) //
						.setDelayChargeRiskLevel(DelayChargeRiskLevel.MEDIUM) //
						.setMode(Mode.AUTOMATIC) //
						.setSellToGridLimitEnabled(true) //
						.setSellToGridLimitRampPercentage(5) //
						.setManualTargetTime("") //
						.build()) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.input(METER_ACTIVE_POWER, 0) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(ESS_CAPACITY, 10_000) //
						.input(ESS_SOC, 20) //
						.input(ESS_MAX_APPARENT_POWER, 10_000) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(PREDICTED_TARGET_MINUTE, /* QuarterHour */ 68 * 15) //
						.output(PREDICTED_TARGET_MINUTE_ADJUSTED, /* QuarterHour */ 68 * 15 - 120) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.ACTIVE_LIMIT) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, -6650) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, 6650) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2700) //
						.output(RAW_DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2700)) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.input(ESS_SOC, 21) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.ACTIVE_LIMIT) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2683) //
						.output(RAW_DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2666)) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2677) //
						.output(RAW_DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2666)) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2675) //
						.output(RAW_DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2666)) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2673) //
						.output(RAW_DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2666)) //
		;
	}

	@Test
	public void automatic_default_predictions_at_evening_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T20:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictions
		final var productionPrediction = new DummyPrediction24Hours(DEFAULT_PRODUCTION_PREDICTION);
		final var consumptionPrediction = new DummyPrediction24Hours(DEFAULT_CONSUMPTION_PREDICTION);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, consumptionPrediction,
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ProductionActivePower")).getValues()));
		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ConsumptionActivePower")).getValues()));

		new ControllerTest(new GridOptimizedChargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS) //
				.addReference("meter", METER) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMaximumSellToGridPower(7_000) //
						.setMeterId(METER_ID) //
						.setDelayChargeRiskLevel(DelayChargeRiskLevel.MEDIUM) //
						.setMode(Mode.AUTOMATIC) //
						.setSellToGridLimitEnabled(true) //
						.setSellToGridLimitRampPercentage(5) //
						.setManualTargetTime("") //
						.build()) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, 0) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(ESS_CAPACITY, 10_000) //
						.input(ESS_SOC, 20) //
						.input(ESS_MAX_APPARENT_POWER, 10_000) //
						.input(PREDICTED_TARGET_MINUTE, /* QuarterHour */ 68 * 15) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.input(PREDICTED_TARGET_MINUTE_ADJUSTED, /* QuarterHour */ 68 * 15 - 120) //
						.input(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2000) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.ACTIVE_LIMIT) //
						// Value increases steadily by 0.25% of max apparent power 10_000
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2025))
				.next(new TestCase() //
						.input(PREDICTED_TARGET_MINUTE, /* QuarterHour */ 68 * 15) //
						.input(PREDICTED_TARGET_MINUTE_ADJUSTED, /* QuarterHour */ 68 * 15 - 120) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(PREDICTED_TARGET_MINUTE, /* QuarterHour */ 68 * 15) //
						.output(PREDICTED_TARGET_MINUTE_ADJUSTED, /* QuarterHour */ 68 * 15 - 120) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.ACTIVE_LIMIT) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, -6650) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, 6650) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2050))
				.next(new TestCase() //
						.input(PREDICTED_TARGET_MINUTE, /* QuarterHour */ 68 * 15) //
						.input(PREDICTED_TARGET_MINUTE_ADJUSTED, /* QuarterHour */ 68 * 15 - 120) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(PREDICTED_TARGET_MINUTE, /* QuarterHour */ 68 * 15) //
						.output(PREDICTED_TARGET_MINUTE_ADJUSTED, /* QuarterHour */ 68 * 15 - 120) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.ACTIVE_LIMIT) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, -6650) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, 6650) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2075));
	}

	@Test
	public void automatic_no_predictions_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictions
		final var productionPrediction = new DummyPrediction24Hours();
		final var consumptionPrediction = new DummyPrediction24Hours();

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, consumptionPrediction,
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		new ControllerTest(new GridOptimizedChargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS) //
				.addReference("meter", METER) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMaximumSellToGridPower(7_000) //
						.setMeterId(METER_ID) //
						.setDelayChargeRiskLevel(DelayChargeRiskLevel.MEDIUM) //
						.setMode(Mode.AUTOMATIC) //
						.setSellToGridLimitEnabled(true) //
						.setSellToGridLimitRampPercentage(5) //
						.setManualTargetTime("") //
						.build()) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, 0) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(ESS_CAPACITY, 10_000) //
						.input(ESS_SOC, 20) //
						.input(ESS_MAX_APPARENT_POWER, 10_000) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, -6650) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, 6650) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.TARGET_MINUTE_NOT_CALCULATED));
	}

	@Test
	public void automatic_sell_to_grid_limit_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, new DummyPrediction24Hours(),
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, new DummyPrediction24Hours(),
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		new ControllerTest(new GridOptimizedChargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS) //
				.addReference("meter", METER) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMaximumSellToGridPower(7_000) //
						.setMeterId(METER_ID) //
						.setDelayChargeRiskLevel(DelayChargeRiskLevel.MEDIUM) //
						.setMode(Mode.AUTOMATIC) //
						.setSellToGridLimitEnabled(true) //
						.setSellToGridLimitRampPercentage(5) //
						.setManualTargetTime("") //
						.build()) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -7500) //
						.input(ESS_CAPACITY, 10_000) //
						.input(ESS_SOC, 20) //
						.input(ESS_MAX_APPARENT_POWER, 10_000) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.TARGET_MINUTE_NOT_CALCULATED) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, null) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -850) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -850) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 850) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -12000) //
						.input(ESS_ACTIVE_POWER, -850) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6200) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6200) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6200) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -7000) //
						.input(ESS_ACTIVE_POWER, -6200) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6550) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6550) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6550) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -5000) //
						.input(ESS_ACTIVE_POWER, -6550) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6050) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6050) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6050) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -8000) //
						.input(ESS_ACTIVE_POWER, -6050) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -7400) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -7400) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 7400) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						// Difference between last limit and current lower than the ramp - ramp is not
						// applied
						.input(METER_ACTIVE_POWER, -7000) //
						.input(ESS_ACTIVE_POWER, -7400) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -7750) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -7750) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 7750) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -6000) //
						.input(ESS_ACTIVE_POWER, -7750) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -7250) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -7250) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 7250) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT));
	}

	@Test
	public void automatic_sell_to_grid_limit_test_with_full_ess() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, new DummyPrediction24Hours(),
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, new DummyPrediction24Hours(),
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		new ControllerTest(new GridOptimizedChargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS) //
				.addReference("meter", METER) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMaximumSellToGridPower(7_000) //
						.setMeterId(METER_ID) //
						.setDelayChargeRiskLevel(DelayChargeRiskLevel.MEDIUM) //
						.setMode(Mode.AUTOMATIC) //
						.setSellToGridLimitEnabled(true) //
						.setSellToGridLimitRampPercentage(5) //
						.setManualTargetTime("") //
						.build()) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -7500) //
						.input(ESS_CAPACITY, 10_000) //
						.input(ESS_SOC, 100) //
						.input(ESS_MAX_APPARENT_POWER, 10_000) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.TARGET_MINUTE_NOT_CALCULATED) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, null) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -500) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -500) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 500) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -12000) //
						.input(ESS_ACTIVE_POWER, -1000) //
						.input(ESS_SOC, 100) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6000) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6000) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6000) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -7000) //
						.input(ESS_ACTIVE_POWER, -6000) //
						.input(ESS_SOC, 100) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6000) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6000) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6000) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -5000) //
						.input(ESS_ACTIVE_POWER, -6000) //
						.input(ESS_SOC, 100) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -5500) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -5500) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 5500) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -8000) //
						.input(ESS_ACTIVE_POWER, -5500) //
						.input(ESS_SOC, 100) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6500) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6500) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6500) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						// Difference between last limit and current lower than the ramp - ramp is not
						// applied
						.input(METER_ACTIVE_POWER, -7000) //
						.input(ESS_ACTIVE_POWER, -6300) //
						.input(ESS_SOC, 100) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6300) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6300) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6300) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -6000) //
						.input(ESS_ACTIVE_POWER, -6000) //
						.input(ESS_SOC, 100) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -5800) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -5800) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 5800) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT));
	}

	@Test
	public void automatic_sell_to_grid_limit_buffer_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, new DummyPrediction24Hours(),
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, new DummyPrediction24Hours(),
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		new ControllerTest(new GridOptimizedChargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS) //
				.addReference("meter", METER) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMaximumSellToGridPower(7_000) //
						.setMeterId(METER_ID) //
						.setDelayChargeRiskLevel(DelayChargeRiskLevel.MEDIUM) //
						.setMode(Mode.AUTOMATIC) //
						.setSellToGridLimitEnabled(true) //
						.setSellToGridLimitRampPercentage(5) //
						.setManualTargetTime("") //
						.build()) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -7500) //
						.input(ESS_CAPACITY, 10_000) //
						.input(ESS_SOC, 20) //
						.input(ESS_MAX_APPARENT_POWER, 10_000) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.TARGET_MINUTE_NOT_CALCULATED) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, null) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -850) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -850) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 850) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -12000) //
						.input(ESS_ACTIVE_POWER, -1000) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6350) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6350) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6350) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -7000) //
						.input(ESS_ACTIVE_POWER, -6000) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6350) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6350) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6350) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -5000) //
						.input(ESS_ACTIVE_POWER, -6000) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -5850) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -5850) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 5850) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -8000) //
						.input(ESS_ACTIVE_POWER, -5500) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6850) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6850) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6850) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						// Difference between last limit and current lower than the ramp - ramp is not
						// applied
						.input(METER_ACTIVE_POWER, -7000) //
						.input(ESS_ACTIVE_POWER, -6300) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6650) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6650) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6650) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -6000) //
						.input(ESS_ACTIVE_POWER, -6000) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6150) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6150) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6150) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT));
	}

	@Test
	public void manual_midnight_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictions
		final var productionPrediction = new DummyPrediction24Hours(DEFAULT_PRODUCTION_PREDICTION);
		final var consumptionPrediction = new DummyPrediction24Hours(DEFAULT_CONSUMPTION_PREDICTION);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, consumptionPrediction,
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ProductionActivePower")).getValues()));
		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ConsumptionActivePower")).getValues()));

		new ControllerTest(new GridOptimizedChargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS) //
				.addReference("meter", METER) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMaximumSellToGridPower(7_000) //
						.setMeterId(METER_ID) //
						.setDelayChargeRiskLevel(DelayChargeRiskLevel.MEDIUM) //
						.setMode(Mode.MANUAL) //
						.setSellToGridLimitEnabled(true) //
						.setManualTargetTime("17:00") //
						.setSellToGridLimitRampPercentage(5) //
						.build()) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, 0) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(ESS_CAPACITY, 10_000) //
						.input(ESS_SOC, 20) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.input(ESS_MAX_APPARENT_POWER, 10_000)) //
				.next(new TestCase() //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(TARGET_MINUTE, /* QuarterHour */ 1020) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.ACTIVE_LIMIT) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, 6650) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, -6650) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 476));
	}

	@Test
	public void manual_midday_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T12:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictions
		final var productionPrediction = new DummyPrediction24Hours(DEFAULT_PRODUCTION_PREDICTION);
		final var consumptionPrediction = new DummyPrediction24Hours(DEFAULT_CONSUMPTION_PREDICTION);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, consumptionPrediction,
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ProductionActivePower")).getValues()));
		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ConsumptionActivePower")).getValues()));

		new ControllerTest(new GridOptimizedChargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS) //
				.addReference("meter", METER) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMaximumSellToGridPower(7_000) //
						.setMeterId(METER_ID) //
						.setDelayChargeRiskLevel(DelayChargeRiskLevel.MEDIUM) //
						.setMode(Mode.MANUAL) //
						.setSellToGridLimitEnabled(true) //
						.setManualTargetTime("17:00") //
						.setSellToGridLimitRampPercentage(5) //
						.build()) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, 0) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(ESS_CAPACITY, 10_000) //
						.input(ESS_SOC, 20) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.input(ESS_MAX_APPARENT_POWER, 10_000) //
						.output(TARGET_MINUTE, /* QuarterHour */ 1020) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.ACTIVE_LIMIT) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, 6650) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, -6650) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 1620));
	}

	@Test
	public void hybridEss_manual_midday_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T12:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictions
		final var productionPrediction = new DummyPrediction24Hours(DEFAULT_PRODUCTION_PREDICTION);
		final var consumptionPrediction = new DummyPrediction24Hours(DEFAULT_CONSUMPTION_PREDICTION);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, consumptionPrediction,
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ProductionActivePower")).getValues()));
		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ConsumptionActivePower")).getValues()));

		new ControllerTest(new GridOptimizedChargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", HYBRID_ESS) //
				.addReference("meter", METER) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMaximumSellToGridPower(7_000) //
						.setMeterId(METER_ID) //
						.setDelayChargeRiskLevel(DelayChargeRiskLevel.MEDIUM) //
						.setMode(Mode.MANUAL) //
						.setSellToGridLimitEnabled(true) //
						.setManualTargetTime("17:00") //
						.setSellToGridLimitRampPercentage(5) //
						.build()) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, 0) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(ESS_CAPACITY, 10_000) //
						.input(ESS_SOC, 20) //
						.input(ESS_MAX_APPARENT_POWER, 10_000) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.input(SUM_PRODUCTION_DC_ACTUAL_POWER, 10_000).output(TARGET_MINUTE, /* QuarterHour */ 1020) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.NO_CHARGE_LIMIT) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 3350) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, 6650) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_FIXED) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, null));

	}

	@Test
	public void mode_off_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictions
		final var productionPrediction = new DummyPrediction24Hours(DEFAULT_PRODUCTION_PREDICTION);
		final var consumptionPrediction = new DummyPrediction24Hours(DEFAULT_CONSUMPTION_PREDICTION);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, consumptionPrediction,
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		new ControllerTest(new GridOptimizedChargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS) //
				.addReference("meter", METER) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMaximumSellToGridPower(7_000) //
						.setMeterId(METER_ID) //
						.setDelayChargeRiskLevel(DelayChargeRiskLevel.MEDIUM) //
						.setMode(Mode.OFF) //
						.setSellToGridLimitEnabled(true) //
						.setSellToGridLimitRampPercentage(5) //
						.setManualTargetTime("") //
						.build()) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -7500) //
						.input(ESS_MAX_APPARENT_POWER, 10_000) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.DISABLED) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, null) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -850) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -850) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 850)); //
	}

	@Test
	public void no_capacity_left_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T12:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictions
		final var productionPrediction = new DummyPrediction24Hours(DEFAULT_PRODUCTION_PREDICTION);
		final var consumptionPrediction = new DummyPrediction24Hours(DEFAULT_CONSUMPTION_PREDICTION);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, consumptionPrediction,
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ProductionActivePower")).getValues()));
		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ConsumptionActivePower")).getValues()));

		new ControllerTest(new GridOptimizedChargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS_WITH_NONE_APPARENT_POWER) //
				.addReference("meter", METER) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMaximumSellToGridPower(7_000) //
						.setMeterId(METER_ID) //
						.setDelayChargeRiskLevel(DelayChargeRiskLevel.MEDIUM) //
						.setMode(Mode.AUTOMATIC) //
						.setSellToGridLimitEnabled(true) //
						.setSellToGridLimitRampPercentage(5) //
						.setManualTargetTime("") //
						.build()) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, 0) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(ESS_CAPACITY, 10_000) //
						.input(ESS_SOC, 99) //
						.input(ESS_MAX_APPARENT_POWER, 10_000) //
						.input(START_EPOCH_SECONDS, 1630566000) //

						// ess.getPower().getMinPower() (Maximum allowed charge power) is '0' because
						// the referenced
						// DummyManagedSymmetricEss has an apparent power of zero.
						.output(DELAY_CHARGE_STATE, DelayChargeState.NO_REMAINING_CAPACITY) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, null)); //
	}

	@Test
	public void start_production_no_enough_test() throws Exception {
		// Sleep between every TestCase to make sure that the Channel Values are added
		// to the pastValues Map. This is required because the Channel Value timestamp
		// does not consider the mocked Clock.
		final ThrowingRunnable<Exception> sleep = () -> Thread.sleep(10);

		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictions
		final var productionPrediction = new DummyPrediction24Hours(DEFAULT_PRODUCTION_PREDICTION);
		final var consumptionPrediction = new DummyPrediction24Hours(DEFAULT_CONSUMPTION_PREDICTION);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, consumptionPrediction,
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		new ControllerTest(new GridOptimizedChargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS) //
				.addReference("meter", METER) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMaximumSellToGridPower(7_000) //
						.setMeterId(METER_ID) //
						.setDelayChargeRiskLevel(DelayChargeRiskLevel.MEDIUM) //
						.setMode(Mode.OFF) //
						.setSellToGridLimitEnabled(true) //
						.setSellToGridLimitRampPercentage(5) //
						.setManualTargetTime("") //
						.build()) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 1000) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.NOT_STARTED) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.NOT_STARTED) //
						.output(START_EPOCH_SECONDS, null))
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 0) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.NOT_STARTED) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.NOT_STARTED) //
						.output(START_EPOCH_SECONDS, null))
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 5000) //
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 6000) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.NOT_STARTED) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.NOT_STARTED) //
						.output(START_EPOCH_SECONDS, null)); //
	}

	@Test
	public void start_production_average_test() throws Exception {
		// Sleep between every TestCase to make sure that the Channel Values are added
		// to the pastValues Map. This is required because the Channel Value timestamp
		// does not consider the mocked Clock.
		final ThrowingRunnable<Exception> sleep = () -> Thread.sleep(10);

		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T00:00:00.00Z"), ZoneId.of("Europe/Berlin"));
		final var cm = new DummyComponentManager(clock);

		// Predictions
		final var productionPrediction = new DummyPrediction24Hours(DEFAULT_PRODUCTION_PREDICTION);
		final var consumptionPrediction = new DummyPrediction24Hours(DEFAULT_CONSUMPTION_PREDICTION);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, consumptionPrediction,
				"_sum/ConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		new ControllerTest(new GridOptimizedChargeImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS) //
				.addReference("meter", METER) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMaximumSellToGridPower(7_000) //
						.setMeterId(METER_ID) //
						.setDelayChargeRiskLevel(DelayChargeRiskLevel.MEDIUM) //
						.setMode(Mode.MANUAL) //
						.setSellToGridLimitEnabled(true) //
						.setManualTargetTime("17:00") //
						.setSellToGridLimitRampPercentage(5) //
						.build()) //
				/*
				 * As there was five times zero production, it needs another five cycles with
				 * 2_000 W production to reach the 1_000 W consumption. Because of the
				 * DEFAULT_POWER_BUFFER it needs a sixth & seventh value to have more average
				 * production than consumption + buffer
				 */
				.next(new TestCase("Cycle 1 - no production") //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 1000) //
						.input(START_EPOCH_SECONDS, null) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.NOT_STARTED) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.NOT_STARTED) //
						.output(START_EPOCH_SECONDS, null))
				.next(new TestCase("Cycle 2 - no production") //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 1000) //
						.input(START_EPOCH_SECONDS, null) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.NOT_STARTED) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.NOT_STARTED) //
						.output(START_EPOCH_SECONDS, null))
				.next(new TestCase("Cycle 3 - no production") //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 1000) //
						.input(START_EPOCH_SECONDS, null) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.NOT_STARTED) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.NOT_STARTED) //
						.output(START_EPOCH_SECONDS, null))
				.next(new TestCase("Cycle 4 - no production") //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 1000) //
						.input(START_EPOCH_SECONDS, null) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.NOT_STARTED) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.NOT_STARTED) //
						.output(START_EPOCH_SECONDS, null))
				.next(new TestCase("Cycle 5 - no production") //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) // Avg: 0
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 1000) //
						.input(START_EPOCH_SECONDS, null) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.NOT_STARTED) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.NOT_STARTED) //
						.output(START_EPOCH_SECONDS, null))
				.next(new TestCase("Cycle 6 - 2_000 production") //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 2000) // Avg: 333
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 1000) //
						.input(START_EPOCH_SECONDS, null) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.NOT_STARTED) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.NOT_STARTED) //
						.output(START_EPOCH_SECONDS, null))
				.next(new TestCase("Cycle 7 - 2_000 production") //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 2000) // Avg: 571
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 1000) //
						.input(START_EPOCH_SECONDS, null) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.NOT_STARTED) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.NOT_STARTED) //
						.output(START_EPOCH_SECONDS, null))
				.next(new TestCase("Cycle 8 - 2_000 production") //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 2000) // Avg: 750
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 1000) //
						.input(START_EPOCH_SECONDS, null) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.NOT_STARTED) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.NOT_STARTED) //
						.output(START_EPOCH_SECONDS, null))
				.next(new TestCase("Cycle 9 - 2_000 production") //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 2000) // Avg: 888
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 1000) //
						.input(START_EPOCH_SECONDS, null) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.NOT_STARTED) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.NOT_STARTED) //
						.output(START_EPOCH_SECONDS, null))
				.next(new TestCase("Cycle 10 - 2_000 production") //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 2000) // Avg: 1000
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 1000) //
						.input(START_EPOCH_SECONDS, null) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.NOT_STARTED) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.NOT_STARTED) //
						.output(START_EPOCH_SECONDS, null))
				.next(new TestCase("Cycle 11 - 2_000 production") //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 2000) // Avg: 1090
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 1000) //
						.input(START_EPOCH_SECONDS, null) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.NOT_STARTED) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.NOT_STARTED) //
						.output(START_EPOCH_SECONDS, null)) //
				.next(new TestCase("Cycle 12 - 2_000 production") //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 2000) // Avg: 1166
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 1000) //
						.input(START_EPOCH_SECONDS, null) //
						.input(METER_ACTIVE_POWER, 0) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(ESS_CAPACITY, 10_000) //
						.input(ESS_SOC, 20) //
						.input(ESS_MAX_APPARENT_POWER, 10_000) //

						// Epoch seconds at 2020-01-01 00:00:00: 1577836800 (Clock is not updated every
						// cycle)
						.output(START_EPOCH_SECONDS, 1577836800L) //
						.output(TARGET_MINUTE, /* QuarterHour */ 1020) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.ACTIVE_LIMIT) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, 6650) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, -6650) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 506)); //
	}
}
