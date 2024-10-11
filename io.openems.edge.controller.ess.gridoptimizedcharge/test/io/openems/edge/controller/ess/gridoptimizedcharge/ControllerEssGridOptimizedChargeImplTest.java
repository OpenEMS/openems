package io.openems.edge.controller.ess.gridoptimizedcharge;

import static io.openems.edge.predictor.api.prediction.Prediction.EMPTY_PREDICTION;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import org.junit.Ignore;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingRunnable;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.Plot;
import io.openems.edge.common.test.Plot.AxisFormat;
import io.openems.edge.common.test.Plot.Data;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyHybridEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.test.DummyElectricityMeter;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.test.DummyPredictor;
import io.openems.edge.predictor.api.test.DummyPredictorManager;

public class ControllerEssGridOptimizedChargeImplTest {

	// Ids
	private static final String CTRL_ID = "ctrlGridOptimizedCharge0";
	private static final String PREDICTOR_ID = "predictor0";
	private static final String ESS_ID = "ess0";
	private static final String METER_ID = "meter0";

	// Components
	private static final DummyManagedSymmetricEss ESS = new DummyManagedSymmetricEss(ESS_ID);
	private static final DummyElectricityMeter METER = new DummyElectricityMeter(METER_ID);
	private static final DummyHybridEss HYBRID_ESS = new DummyHybridEss(ESS_ID);
	private static final DummyManagedSymmetricEss ESS_WITH_NONE_APPARENT_POWER = new DummyManagedSymmetricEss(ESS_ID) //
			.withMaxApparentPower(0);

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
		final var now = ZonedDateTime.now(clock);
		final var cm = new DummyComponentManager(clock);
		final var sum = new DummySum();
		final var predictorManager = new DummyPredictorManager(
				// Production
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_PRODUCTION_ACTIVE_POWER, now, DEFAULT_PRODUCTION_PREDICTION),
						SUM_PRODUCTION_ACTIVE_POWER),
				// Consumption
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, now, DEFAULT_CONSUMPTION_PREDICTION),
						SUM_CONSUMPTION_ACTIVE_POWER));

		new ControllerTest(new ControllerEssGridOptimizedChargeImpl()) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, 0) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(ESS_CAPACITY, 10_000) //
						.input(ESS_SOC, 20) //
						.input(ESS_MAX_APPARENT_POWER, 10_000) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(PREDICTED_TARGET_MINUTE, /* QuarterHour */ 68 * 15) //
						.output(PREDICTED_TARGET_MINUTE_ADJUSTED, /* QuarterHour */ 68 * 15 - 120) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.AVOID_LOW_CHARGING) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, -6650) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, 6650) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 0)); // Avoid low charge power
	}

	@Test
	public void automatic_default_predictions_at_midday_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T12:00:00.00Z"), ZoneOffset.UTC);
		final var midnight = ZonedDateTime.now(clock).truncatedTo(DAYS);
		final var cm = new DummyComponentManager(clock);
		final var sum = new DummySum();
		final var predictorManager = new DummyPredictorManager(
				// Production
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_PRODUCTION_ACTIVE_POWER, midnight, DEFAULT_PRODUCTION_PREDICTION),
						SUM_PRODUCTION_ACTIVE_POWER),
				// Consumption
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, midnight, DEFAULT_CONSUMPTION_PREDICTION),
						SUM_CONSUMPTION_ACTIVE_POWER));

		new ControllerTest(new ControllerEssGridOptimizedChargeImpl()) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
						// If Energy calculation would be applied on medium risk level - Predicted
						// available Energy is not enough to reach 100%
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2700));
	}

	@Test
	public void automatic_default_predictions_at_midday_averaged_test() throws Exception {
		// Sleep between every TestCase to make sure that the Channel Values are added
		// to the pastValues Map. This is required because the Channel Value timestamp
		// does not consider the mocked Clock.
		final ThrowingRunnable<Exception> sleep = () -> Thread.sleep(10);

		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T12:00:00.00Z"), ZoneOffset.UTC);
		final var midnight = ZonedDateTime.now(clock).truncatedTo(DAYS);
		final var cm = new DummyComponentManager(clock);
		final var sum = new DummySum();
		final var predictorManager = new DummyPredictorManager(
				// Production
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_PRODUCTION_ACTIVE_POWER, midnight, DEFAULT_PRODUCTION_PREDICTION),
						SUM_PRODUCTION_ACTIVE_POWER),
				// Consumption
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, midnight, DEFAULT_CONSUMPTION_PREDICTION),
						SUM_CONSUMPTION_ACTIVE_POWER));

		new ControllerTest(new ControllerEssGridOptimizedChargeImpl()) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(ESS_SOC, 21) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.ACTIVE_LIMIT) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2683) //
						.output(RAW_DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2666)) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2677) //
						.output(RAW_DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2666)) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2675) //
						.output(RAW_DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2666)) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2673) //
						.output(RAW_DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 2666)) //
		;
	}

	@Test
	public void automatic_default_predictions_at_evening_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T20:00:00.00Z"), ZoneOffset.UTC);
		final var now = ZonedDateTime.now(clock);
		final var cm = new DummyComponentManager(clock);
		final var sum = new DummySum();
		final var predictorManager = new DummyPredictorManager(
				// Production
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_PRODUCTION_ACTIVE_POWER, now, DEFAULT_PRODUCTION_PREDICTION),
						SUM_PRODUCTION_ACTIVE_POWER),
				// Consumption
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, now, DEFAULT_CONSUMPTION_PREDICTION),
						SUM_CONSUMPTION_ACTIVE_POWER));

		new ControllerTest(new ControllerEssGridOptimizedChargeImpl()) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
		final var predictorManager = new DummyPredictorManager(
				new DummyPredictor(PREDICTOR_ID, cm, EMPTY_PREDICTION, SUM_PRODUCTION_ACTIVE_POWER), //
				new DummyPredictor(PREDICTOR_ID, cm, EMPTY_PREDICTION, SUM_CONSUMPTION_ACTIVE_POWER));

		new ControllerTest(new ControllerEssGridOptimizedChargeImpl()) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
		final var predictorManager = new DummyPredictorManager(
				new DummyPredictor(PREDICTOR_ID, cm, EMPTY_PREDICTION, SUM_PRODUCTION_ACTIVE_POWER), //
				new DummyPredictor(PREDICTOR_ID, cm, EMPTY_PREDICTION, SUM_CONSUMPTION_ACTIVE_POWER));

		new ControllerTest(new ControllerEssGridOptimizedChargeImpl()) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, -12000) //
						.input(ESS_ACTIVE_POWER, -850) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6200) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6200) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6200) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, -7000) //
						.input(ESS_ACTIVE_POWER, -6200) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6550) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6550) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6550) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, -5000) //
						.input(ESS_ACTIVE_POWER, -6550) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6050) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6050) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6050) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, -7000) //
						.input(ESS_ACTIVE_POWER, -7400) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -7750) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -7750) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 7750) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
		final var predictorManager = new DummyPredictorManager(
				new DummyPredictor(PREDICTOR_ID, cm, EMPTY_PREDICTION, SUM_PRODUCTION_ACTIVE_POWER), //
				new DummyPredictor(PREDICTOR_ID, cm, EMPTY_PREDICTION, SUM_CONSUMPTION_ACTIVE_POWER));

		new ControllerTest(new ControllerEssGridOptimizedChargeImpl()) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(ESS_ACTIVE_POWER, -1000) //
						.input(ESS_SOC, 100) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6000) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6000) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6000) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -7000) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(ESS_ACTIVE_POWER, -6000) //
						.input(ESS_SOC, 100) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6000) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6000) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6000) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -5000) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(ESS_ACTIVE_POWER, -6000) //
						.input(ESS_SOC, 100) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -5500) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -5500) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 5500) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -8000) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(ESS_ACTIVE_POWER, -6300) //
						.input(ESS_SOC, 100) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6300) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6300) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6300) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
		final var predictorManager = new DummyPredictorManager(
				new DummyPredictor(PREDICTOR_ID, cm, EMPTY_PREDICTION, SUM_PRODUCTION_ACTIVE_POWER), //
				new DummyPredictor(PREDICTOR_ID, cm, EMPTY_PREDICTION, SUM_CONSUMPTION_ACTIVE_POWER));

		new ControllerTest(new ControllerEssGridOptimizedChargeImpl()) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, -12000) //
						.input(ESS_ACTIVE_POWER, -1000) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6350) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6350) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6350) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, -7000) //
						.input(ESS_ACTIVE_POWER, -6000) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6350) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6350) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6350) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, -5000) //
						.input(ESS_ACTIVE_POWER, -6000) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -5850) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -5850) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 5850) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, -7000) //
						.input(ESS_ACTIVE_POWER, -6300) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, -6650) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, -6650) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, 6650) //
						.output(SELL_TO_GRID_LIMIT_STATE, SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT)) //
				.next(new TestCase() //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
		final var now = ZonedDateTime.now(clock);
		final var cm = new DummyComponentManager(clock);
		final var sum = new DummySum();
		final var predictorManager = new DummyPredictorManager(
				// Production
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_PRODUCTION_ACTIVE_POWER, now, DEFAULT_PRODUCTION_PREDICTION),
						SUM_PRODUCTION_ACTIVE_POWER),
				// Consumption
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, now, DEFAULT_CONSUMPTION_PREDICTION),
						SUM_CONSUMPTION_ACTIVE_POWER));

		new ControllerTest(new ControllerEssGridOptimizedChargeImpl()) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
						.input(START_EPOCH_SECONDS, 1630566000) //
						.output(TARGET_MINUTE, /* QuarterHour */ 1020) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.AVOID_LOW_CHARGING) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, 6650) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, -6650) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 0)); // 476 W below minimum
	}

	@Test
	public void manual_midday_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T12:00:00.00Z"), ZoneOffset.UTC);
		final var now = ZonedDateTime.now(clock);
		final var cm = new DummyComponentManager(clock);
		final var sum = new DummySum();
		final var predictorManager = new DummyPredictorManager(
				// Production
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_PRODUCTION_ACTIVE_POWER, now, DEFAULT_PRODUCTION_PREDICTION),
						SUM_PRODUCTION_ACTIVE_POWER),
				// Consumption
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, now, DEFAULT_CONSUMPTION_PREDICTION),
						SUM_CONSUMPTION_ACTIVE_POWER));

		new ControllerTest(new ControllerEssGridOptimizedChargeImpl()) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
		final var now = ZonedDateTime.now(clock);
		final var cm = new DummyComponentManager(clock);
		final var sum = new DummySum();
		final var predictorManager = new DummyPredictorManager(
				// Production
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_PRODUCTION_ACTIVE_POWER, now, DEFAULT_PRODUCTION_PREDICTION),
						SUM_PRODUCTION_ACTIVE_POWER),
				// Consumption
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, now, DEFAULT_CONSUMPTION_PREDICTION),
						SUM_CONSUMPTION_ACTIVE_POWER));

		new ControllerTest(new ControllerEssGridOptimizedChargeImpl()) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
		final var now = ZonedDateTime.now(clock);
		final var cm = new DummyComponentManager(clock);
		final var sum = new DummySum();
		final var predictorManager = new DummyPredictorManager(
				// Production
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_PRODUCTION_ACTIVE_POWER, now, DEFAULT_PRODUCTION_PREDICTION),
						SUM_PRODUCTION_ACTIVE_POWER),
				// Consumption
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, now, DEFAULT_CONSUMPTION_PREDICTION),
						SUM_CONSUMPTION_ACTIVE_POWER));

		new ControllerTest(new ControllerEssGridOptimizedChargeImpl()) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
		final var now = ZonedDateTime.now(clock);
		final var cm = new DummyComponentManager(clock);
		final var sum = new DummySum();
		final var predictorManager = new DummyPredictorManager(
				// Production
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_PRODUCTION_ACTIVE_POWER, now, DEFAULT_PRODUCTION_PREDICTION),
						SUM_PRODUCTION_ACTIVE_POWER),
				// Consumption
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, now, DEFAULT_CONSUMPTION_PREDICTION),
						SUM_CONSUMPTION_ACTIVE_POWER));

		new ControllerTest(new ControllerEssGridOptimizedChargeImpl()) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 0) //
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
	public void start_production_not_enough_test() throws Exception {
		// Sleep between every TestCase to make sure that the Channel Values are added
		// to the pastValues Map. This is required because the Channel Value timestamp
		// does not consider the mocked Clock.
		final ThrowingRunnable<Exception> sleep = () -> Thread.sleep(10);

		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var now = ZonedDateTime.now(clock);
		final var cm = new DummyComponentManager(clock);
		final var sum = new DummySum();
		final var predictorManager = new DummyPredictorManager(
				// Production
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_PRODUCTION_ACTIVE_POWER, now, DEFAULT_PRODUCTION_PREDICTION),
						SUM_PRODUCTION_ACTIVE_POWER),
				// Consumption
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, now, DEFAULT_CONSUMPTION_PREDICTION),
						SUM_CONSUMPTION_ACTIVE_POWER));

		new ControllerTest(new ControllerEssGridOptimizedChargeImpl()) //
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
		final ThrowingRunnable<Exception> sleep = () -> Thread.sleep(50);

		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T00:00:00.00Z"), ZoneId.of("Europe/Berlin"));
		final var now = ZonedDateTime.now(clock);
		final var cm = new DummyComponentManager(clock);
		final var sum = new DummySum();
		final var predictorManager = new DummyPredictorManager(
				// Production
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_PRODUCTION_ACTIVE_POWER, now, DEFAULT_PRODUCTION_PREDICTION),
						SUM_PRODUCTION_ACTIVE_POWER),
				// Consumption
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, now, DEFAULT_CONSUMPTION_PREDICTION),
						SUM_CONSUMPTION_ACTIVE_POWER));

		new ControllerTest(new ControllerEssGridOptimizedChargeImpl()) //
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
						.input(SUM_PRODUCTION_ACTIVE_POWER, 1900) // Avg: 1090
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

						// Epoch seconds at 2020-01-01 00:00:00: 1577836800 (Clock is not updated)
						.output(START_EPOCH_SECONDS, 1577836800L) //
						.output(TARGET_MINUTE, /* QuarterHour */ 1020) //
						.output(DELAY_CHARGE_STATE, DelayChargeState.AVOID_LOW_CHARGING) //
						.output(RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT, 6650) //
						.output(SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT, -6650) //
						.output(DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT, 0)); // 506 W is not efficient
	}

	@Test
	public void getCalculatedPowerLimit_middayTest() throws Exception {

		/*
		 * Initial values
		 */
		TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T12:00:00.00Z"), ZoneOffset.UTC);
		final int targetMinute = ControllerEssGridOptimizedChargeImplTest.getValidTargetMinute("17:00");
		final DelayChargeRiskLevel riskLevel = DelayChargeRiskLevel.MEDIUM;
		final int maxApparentPower = 10_000;
		final int soc = 20;
		final int capacity = 10_000;
		final ControllerEssGridOptimizedChargeImpl parent = new ControllerEssGridOptimizedChargeImpl();

		// Minimum charge power, to avoid low charge power
		int minimumPower = Math.round(capacity * 0.081F);

		/*
		 * Calculated values (Calculate the remaining capacity with soc minus one, to
		 * avoid very high results at the end.)
		 */
		final int remainingCapacity = Math.round(capacity * (100 - (soc - 1)) * 36);
		final int remainingTime = DelayCharge.calculateRemainingTime(clock, targetMinute);

		Integer maximumChargePower = DelayCharge.getCalculatedPowerLimit(remainingCapacity, remainingTime,
				DEFAULT_PRODUCTION_PREDICTION, DEFAULT_CONSUMPTION_PREDICTION, clock, riskLevel, maxApparentPower,
				targetMinute, minimumPower, parent);

		// Expected energy is to low ()
		// assertNull(maximumChargePower);
		if (maximumChargePower == null) {
			fail("No limit is applied");
		}

		// If Energy calculation would be applied on medium risk level - Predicted
		// available Energy is not enough to reach 100%
		assertEquals(1620, (int) maximumChargePower); //
	}

	private static final Integer[] PRODUCTION_PREDICTION_LOW = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 74, 297, 610, 913, 1399, 1838, 2261, 2662, 3052, 3405, 3708, 4011, 4270,
			4458, 4630, 4794, 4908, 4963, 4960, 4973, 4940, 4859, 4807, 4698, 4530, 4348, 4147, 3527, 3141, 2917, 2703,
			2484, 2233, 1971, 1674, 1386, 1089, 811, 531, 298, 143, 41, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0,
			/* Second day */
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 130, 402, 667, 1023,
			1631, 2020, 2420, 2834, 3237, 3638, 4006, 4338, 4597, 4825, 4965, 5111, 5213, 5268, 5317, 5321, 5271, 5232,
			5193, 5044, 4915, 4738, 4499, 3702, 3226, 3046, 2857, 2649, 2421, 2184, 1933, 1674, 1364, 1070, 754, 447,
			193, 52, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	private static final Integer[] PRODUCTION_PREDICTION_MEDIUM = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 2, 15, 122, 545, 990, 1432, 1823, 2418, 3196, 3794, 4309, 4844, 5359, 5939, 6605, 7193,
			7701, 8238, 8669, 9103, 9438, 9727, 9898, 9965, 10056, 10060, 10076, 9939, 9914, 9778, 9561, 9101, 8708,
			8316, 8242, 8326, 7852, 7417, 6955, 6340, 5614, 4755, 3735, 2376, 1510, 912, 476, 90, 22, 2, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			/* Second day */
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 130, 402, 667, 1023,
			1631, 2020, 2420, 2834, 3237, 3638, 4006, 4338, 4597, 4825, 4965, 5111, 5213, 5268, 5317, 5321, 5271, 5232,
			5193, 5044, 4915, 4738, 4499, 3702, 3226, 3046, 2857, 2649, 2421, 2184, 1933, 1674, 1364, 1070, 754, 447,
			193, 52, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	private static final Integer[] PRODUCTION_PREDICTION_HIGH = { //
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 484, 764, 871, 3327, 3907,
			5664, 8527, 8767, 11869, 15731, 21818, 24061, 26550, 29918, 32624, 34673, 36278, 37681, 38966, 39906, 40911,
			41710, 42093, 42042, 41708, 39322, 36458, 33311, 29743, 26243, 20648, 13196, 11178, 7555, 5808, 4758, 3778,
			2554, 2210, 1730, 1415, 1089, 787, 504, 71, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0,
			/* Second day */
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 484, 764, 871, 3327, 3907,
			5664, 8527, 8767, 11869, 15731, 21818, 24061, 26550, 29918, 32624, 34673, 36278, 37681, 38966, 39906, 40911,
			41710, 42093, 42042, 41708, 39322, 36458, 33311, 29743, 26243, 20648, 13196, 11178, 7555, 5808, 4758, 3778,
			2554, 2210, 1730, 1415, 1089, 787, 504, 71, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0 };

	private static final Integer[] PRODUCTION_PREDICTION_MEDIUM_EARLY = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 2, 15, 122, 545, 990, 1432, 1823, 2418, 3196, 3794, 4309, 4844, 5359, 5939, 6605, 7193, 7701,
			8238, 8669, 9103, 9438, 9727, 9898, 9965, 10056, 10060, 10076, 9939, 9914, 9778, 9561, 9101, 8708, 8316,
			8242, 8326, 7852, 7417, 6955, 6340, 5614, 4755, 3735, 2376, 1510, 912, 476, 90, 22, 2, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			/* Second day */
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 130, 402, 667, 1023,
			1631, 2020, 2420, 2834, 3237, 3638, 4006, 4338, 4597, 4825, 4965, 5111, 5213, 5268, 5317, 5321, 5271, 5232,
			5193, 5044, 4915, 4738, 4499, 3702, 3226, 3046, 2857, 2649, 2421, 2184, 1933, 1674, 1364, 1070, 754, 447,
			193, 52, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	private static final Integer[] PRODUCTION_PREDICTION_MEDIUM_LATE = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 15, 122, 545, 990, 1432, 1823, 2418, 3196, 3794, 4309, 4844, 5359,
			5939, 6605, 7193, 7701, 8238, 8669, 9103, 9438, 9727, 9898, 9965, 10056, 10060, 10076, 9939, 9914, 9778,
			9561, 9101, 8708, 8316, 8242, 8326, 7852, 7417, 6955, 6340, 5614, 4755, 3735, 2376, 1510, 912, 476, 90, 22,
			2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			/* Second day */
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 130, 402, 667, 1023,
			1631, 2020, 2420, 2834, 3237, 3638, 4006, 4338, 4597, 4825, 4965, 5111, 5213, 5268, 5317, 5321, 5271, 5232,
			5193, 5044, 4915, 4738, 4499, 3702, 3226, 3046, 2857, 2649, 2421, 2184, 1933, 1674, 1364, 1070, 754, 447,
			193, 52, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	private static final Integer[] CONSUMPTION_PREDICTION_MIDDLE = { 1021, 1208, 713, 931, 2847, 2551, 1558, 1234, 433,
			633, 1355, 606, 430, 1432, 1121, 502, 294, 1048, 1194, 914, 1534, 1226, 1235, 977, 578, 1253, 1983, 1417,
			513, 929, 1102, 445, 1208, 2791, 2729, 2609, 2086, 1454, 848, 816, 2610, 3150, 2036, 1180, 359, 1316, 3447,
			2104, 905, 802, 828, 812, 863, 633, 293, 379, 296, 296, 436, 140, 135, 196, 230, 175, 365, 758, 325, 264,
			181, 167, 228, 1082, 777, 417, 798, 1268, 409, 830, 1191, 417, 1087, 2958, 2946, 2235, 1343, 483, 796, 1201,
			567, 395, 989, 1066, 370, 989, 1255, 660,
			/* Second day */
			349, 880, 1186, 580, 327, 911, 1135, 553, 265, 938, 1165, 567, 278, 863, 1239, 658, 236, 816, 1173, 1131,
			498, 550, 1344, 1226, 874, 504, 1733, 1809, 1576, 369, 771, 2583, 3202, 2174, 1878, 2132, 2109, 1895, 1565,
			1477, 1613, 1716, 1867, 1726, 1700, 1787, 1755, 1734, 1380, 691, 338, 168, 199, 448, 662, 205, 183, 70, 169,
			276, 149, 76, 195, 168, 159, 266, 135, 120, 224, 979, 2965, 1337, 1116, 795, 334, 390, 433, 369, 762, 2908,
			3226, 2358, 1778, 1002, 455, 654, 534, 1587, 1638, 459, 330, 258, 368, 728, 1096, 878 };

	@Test
	@Ignore // Avoid creating files in every automatic build
	public void getCalculatedPowerLimit_wholeDayTest() throws Exception {

		/*
		 * Initial values
		 */
		// Is calculated automatically if Method is called with targetMinute =
		// Optional.empty()
		// int targetMinute = MyControllerTest.getValidTargetMinute("17:00");
		int maxApparentPower = 10_000;
		int allowedChargePower = 10_000;
		int soc = 90;
		float resultBuffer = 0.5f; // SoC

		// PREDICTION HIGH

		/*
		 * Prediction is equals to actual production
		 */
		this.testLogic("01 Pred_High_Act_High", PRODUCTION_PREDICTION_HIGH, CONSUMPTION_PREDICTION_MIDDLE, soc,
				Optional.empty(), maxApparentPower, allowedChargePower, PRODUCTION_PREDICTION_HIGH,
				CONSUMPTION_PREDICTION_MIDDLE, resultBuffer);

		/*
		 * Prediction is higher than actual production
		 */
		this.testLogic("02 Pred_High_Act_Medium", PRODUCTION_PREDICTION_HIGH, CONSUMPTION_PREDICTION_MIDDLE, soc,
				Optional.empty(), maxApparentPower, allowedChargePower, PRODUCTION_PREDICTION_MEDIUM,
				CONSUMPTION_PREDICTION_MIDDLE, resultBuffer);

		/*
		 * Prediction is higher than actual production
		 */
		this.testLogic("03 Pred_High_Act_Low", PRODUCTION_PREDICTION_HIGH, CONSUMPTION_PREDICTION_MIDDLE, soc,
				Optional.empty(), maxApparentPower, allowedChargePower, PRODUCTION_PREDICTION_LOW,
				CONSUMPTION_PREDICTION_MIDDLE, resultBuffer);

		// PREDICTION MEDIUM

		/*
		 * Prediction is medium and equals to actual production
		 */
		this.testLogic("04 Pred_Medium_Act_Low", PRODUCTION_PREDICTION_MEDIUM, CONSUMPTION_PREDICTION_MIDDLE, soc,
				Optional.empty(), maxApparentPower, allowedChargePower, PRODUCTION_PREDICTION_LOW,
				CONSUMPTION_PREDICTION_MIDDLE, resultBuffer);

		/*
		 * Prediction is lower than actual production
		 */
		this.testLogic("05 Pred_Medium_Act_Medium", PRODUCTION_PREDICTION_MEDIUM, CONSUMPTION_PREDICTION_MIDDLE, soc,
				Optional.empty(), maxApparentPower, allowedChargePower, PRODUCTION_PREDICTION_MEDIUM,
				CONSUMPTION_PREDICTION_MIDDLE, resultBuffer);

		/*
		 * Prediction is higher than actual production
		 */
		this.testLogic("06 Pred_Medium_Act_High", PRODUCTION_PREDICTION_MEDIUM, CONSUMPTION_PREDICTION_MIDDLE, soc,
				Optional.empty(), maxApparentPower, allowedChargePower, PRODUCTION_PREDICTION_HIGH,
				CONSUMPTION_PREDICTION_MIDDLE, resultBuffer);

		// PREDICTION LOW

		/*
		 * Prediction is low and equals to actual production
		 */
		this.testLogic("07 Pred_Low_Act_Low", PRODUCTION_PREDICTION_LOW, CONSUMPTION_PREDICTION_MIDDLE, soc,
				Optional.empty(), maxApparentPower, allowedChargePower, PRODUCTION_PREDICTION_LOW,
				CONSUMPTION_PREDICTION_MIDDLE, resultBuffer);

		/*
		 * Prediction is lower than actual production
		 */
		this.testLogic("08 Pred_Low_Act_Medium", PRODUCTION_PREDICTION_LOW, CONSUMPTION_PREDICTION_MIDDLE, soc,
				Optional.empty(), maxApparentPower, allowedChargePower, PRODUCTION_PREDICTION_MEDIUM,
				CONSUMPTION_PREDICTION_MIDDLE, resultBuffer);

		/*
		 * Prediction is lower than actual production
		 */
		this.testLogic("09 Pred_Low_Act_High", PRODUCTION_PREDICTION_LOW, CONSUMPTION_PREDICTION_MIDDLE, soc,
				Optional.empty(), maxApparentPower, allowedChargePower, PRODUCTION_PREDICTION_HIGH,
				CONSUMPTION_PREDICTION_MIDDLE, resultBuffer);

		// PREDICTION shifted to morning or evening

		/*
		 * Prediction is earlier than actual production
		 */
		this.testLogic("10 Pred_Early_Act_Medium", PRODUCTION_PREDICTION_MEDIUM_EARLY, CONSUMPTION_PREDICTION_MIDDLE,
				soc, Optional.empty(), maxApparentPower, allowedChargePower, PRODUCTION_PREDICTION_MEDIUM,
				CONSUMPTION_PREDICTION_MIDDLE, resultBuffer);

		/*
		 * Prediction is later than actual production
		 */
		this.testLogic("11 Pred_Late_Act_Medium", PRODUCTION_PREDICTION_MEDIUM_LATE, CONSUMPTION_PREDICTION_MIDDLE, soc,
				Optional.empty(), maxApparentPower, allowedChargePower, PRODUCTION_PREDICTION_MEDIUM,
				CONSUMPTION_PREDICTION_MIDDLE, resultBuffer);
	}

	private void testLogic(String description, Integer[] productionPrediction, Integer[] consumptionPrediction, int soc,
			Optional<Integer> targetMinuteOpt, int maxApparentPower, int allowedChargePower, Integer[] productionActual,
			Integer[] consumptionActual, float resultBuffer) {

		Map<String, Integer> capacities = Map.of(//
				"01 Low_Cap", 10_000, //
				"02 Medium_Cap", 20_000, //
				"03 High_Cap", 44_000);

		Map<String, DelayChargeRiskLevel> riskLevel = Map.of(//
				"01 Low_Risk", DelayChargeRiskLevel.LOW, //
				"02 Medium_Risk", DelayChargeRiskLevel.MEDIUM, //
				"03 High_Risk", DelayChargeRiskLevel.HIGH);

		capacities.forEach((capKey, capVal) -> {
			riskLevel.forEach((riskKey, riskVal) -> {
				String dynamicPath = description + "/" + capKey + "/" + riskKey;
				File path = new File("./testResults/New/" + description + "/" + capKey + "/" + riskKey);
				File pathOld = new File("./testResults/Old/" + description + "/" + capKey + "/" + riskKey);

				// Create paths on first execute
				if (!path.exists()) {
					path.mkdirs();
				}
				if (!pathOld.exists()) {
					pathOld.mkdirs();
				}

				// Test one day with all variants
				this.testOneDay(dynamicPath, productionPrediction, consumptionPrediction, soc, targetMinuteOpt, capVal,
						maxApparentPower, allowedChargePower, riskVal, productionActual, consumptionActual,
						resultBuffer);
			});
		});
	}

	private DelayChargeResultState testOneDay(String testDescription, Integer[] productionPrediction,
			Integer[] consumptionPrediction, int soc, Optional<Integer> targetMinuteOpt, int capacity,
			int maxApparentPower, int allowedChargePower, DelayChargeRiskLevel riskLevel, Integer[] productionActual,
			Integer[] consumptionActual, float resultBuffer) {
		DelayChargeResultState resultState;
		DelayChargeResult newLogic = ControllerEssGridOptimizedChargeImplTest.testOneDay(testDescription,
				productionPrediction, consumptionPrediction, soc, targetMinuteOpt, capacity, maxApparentPower,
				allowedChargePower, riskLevel, productionActual, consumptionActual, false);

		DelayChargeResult oldLogic = ControllerEssGridOptimizedChargeImplTest.testOneDay(testDescription,
				productionPrediction, consumptionPrediction, soc, targetMinuteOpt, capacity, maxApparentPower,
				allowedChargePower, riskLevel, productionActual, consumptionActual, true);

		if (newLogic.getFinalSoc() + resultBuffer < oldLogic.getFinalSoc()) {
			resultState = DelayChargeResultState.WARNING;
		} else if (newLogic.getFinalSoc() - resultBuffer > oldLogic.getFinalSoc()) {
			resultState = DelayChargeResultState.PERFECT;
		} else {
			resultState = DelayChargeResultState.OK;
		}

		float unefficientEnergy = Math
				.round(newLogic.getChargedEnergyWithLowPower() / newLogic.getChargedEnergy() * 1000) / 10.0f;
		float unefficientEnergyOld = Math
				.round(oldLogic.getChargedEnergyWithLowPower() / oldLogic.getChargedEnergy() * 1000) / 10.0f;
		System.out.println(resultState.text + "\t" + testDescription + "     \t(New: "
				+ Math.round(newLogic.getFinalSoc() * 100) / 100.0 + " | Old: "
				+ Math.round(oldLogic.getFinalSoc() * 100) / 100.0 + ") \t   Energy: (New: "
				+ newLogic.getChargedEnergy() + "[" + newLogic.getChargedEnergyWithLowPower() + " -> "
				+ unefficientEnergy + "%] | Old: " + oldLogic.getChargedEnergy() + "["
				+ oldLogic.getChargedEnergyWithLowPower() + " -> " + unefficientEnergyOld + "%])");

		// fail("New logic results in a lower SoC (New: " + newLogic.getFinalSoc() + " |
		// Old: "+ oldLogic.getFinalSoc() + ") - " + testDescription);
		return resultState;
	}

	private static class DelayChargeResult {

		private float finalSoc;
		private float chargedEnergy;
		private float chargedEnergyWithLowPower;

		public DelayChargeResult(float finalSoc, float chargedEnergy, float chargedEnergyWithLowPower) {
			this.finalSoc = finalSoc;
			this.chargedEnergy = chargedEnergy;
			this.chargedEnergyWithLowPower = chargedEnergyWithLowPower;
		}

		public float getFinalSoc() {
			return this.finalSoc;
		}

		public float getChargedEnergy() {
			return this.chargedEnergy;
		}

		public float getChargedEnergyWithLowPower() {
			return this.chargedEnergyWithLowPower;
		}
	}

	private static enum DelayChargeResultState {
		OK("OK - SoC as bevore"), //
		WARNING("WARNING - Lower SoC"), //
		PERFECT("PERFECT - Higher SoC");

		private String text;

		DelayChargeResultState(String text) {
			this.text = text;
		}
	}

	@SuppressWarnings("deprecation")
	private static DelayChargeResult testOneDay(String testDescription, Integer[] productionPrediction,
			Integer[] consumptionPrediction, int soc, Optional<Integer> targetMinuteOpt, int capacity,
			int maxApparentPower, int allowedChargePower, DelayChargeRiskLevel riskLevel, Integer[] productionActual,
			Integer[] consumptionActual, boolean oldLogic) {

		Data dataProduction = Plot.data();
		Data dataProductionPrediction = Plot.data();
		Data dataConsumption = Plot.data();
		Data dataConsumptionPrediction = Plot.data();
		Data dataMaximum = Plot.data();
		Data dataChargePower = Plot.data();
		Data dataSoc = Plot.data();

		TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		int targetMinute = targetMinuteOpt.isPresent() ? targetMinuteOpt.get()
				: DelayCharge.calculateTargetMinute(productionPrediction, consumptionPrediction,
						clock.instant().atZone(ZoneOffset.UTC)).orElse(1020);

		int remainingCapacity = 0;
		int remainingTime = 0;
		StringBuilder b = new StringBuilder();
		int totoalActivePower = 0;
		int totoalActivePowerLessEfficiency = 0;
		float socFloat = soc;
		final ControllerEssGridOptimizedChargeImpl parent = new ControllerEssGridOptimizedChargeImpl();

		// Minimum charge power, to avoid low charge power
		int minimumPower = Math.round(capacity * 0.081F);

		for (int i = 0; i < 96; i++) {

			remainingTime = DelayCharge.calculateRemainingTime(clock, targetMinute);
			remainingCapacity = Math.round(capacity * (100 - (socFloat - 1)) * 36);

			Integer maximumChargePower;
			if (oldLogic) {
				maximumChargePower = DelayCharge._getCalculatedPowerLimit(remainingCapacity, remainingTime);
			} else {
				maximumChargePower = DelayCharge.getCalculatedPowerLimit(remainingCapacity, remainingTime,
						productionPrediction, consumptionPrediction, clock, riskLevel, maxApparentPower, targetMinute,
						minimumPower, parent);
			}

			int chargePowerLimit = maximumChargePower == null ? (productionPrediction[i] - consumptionPrediction[i])
					: maximumChargePower;

			int actualPower = Math.min(chargePowerLimit, productionActual[i] - consumptionActual[i]);

			actualPower = actualPower > 0 && socFloat >= 100 ? 0 : actualPower;

			socFloat += (actualPower * 0.25f) / (capacity / 100.0f);

			// Update total power
			if (actualPower > 0) {
				totoalActivePower += actualPower;
				if (actualPower < minimumPower) {
					totoalActivePowerLessEfficiency += actualPower;
				}
			}

			// Fit within min/max
			socFloat = Math.max(0, Math.min(100, socFloat));

			b.append("Limit: " + chargePowerLimit + ", Real: " + actualPower + ", SoC: " + socFloat + "\t");

			// Time is not used for x axis, because smaller range is causing odd lines
			// double time = clock.instant().atZone(ZoneOffset.UTC).getHour() + 0.01 *
			// clock.instant().atZone(ZoneOffset.UTC).getMinute();
			dataSoc.xy(i, socFloat);
			dataMaximum.xy(i, maximumChargePower == null ? 0 : maximumChargePower);
			dataProduction.xy(i, productionActual[i]);
			dataProductionPrediction.xy(i, productionPrediction[i]);
			dataConsumption.xy(i, consumptionActual[i]);
			dataConsumptionPrediction.xy(i, consumptionPrediction[i]);
			dataChargePower.xy(i, actualPower);
			clock = new TimeLeapClock(clock.instant().plus(15, ChronoUnit.MINUTES), ZoneOffset.UTC);
		}

		Plot plot = Plot.plot(//
				Plot.plotOpts() //
						.title(testDescription) //
						.legend(Plot.LegendFormat.BOTTOM)) //
				.xAxis("x", Plot.axisOpts() //
						.format(AxisFormat.NUMBER_INT) //
						.range(0, 96)) //
				.yAxis("y", Plot.axisOpts() //
						.format(AxisFormat.NUMBER_INT)) //
				.series("Production", dataProduction, Plot.seriesOpts() //
						.color(Color.GREEN)) //
				.series("Consumption", dataConsumption, Plot.seriesOpts() //
						.color(Color.YELLOW)) //
				.series("Charging", dataChargePower, Plot.seriesOpts() //
						.color(Color.CYAN)) //
				.series("Maximum Charge", dataMaximum, Plot.seriesOpts() //
						.color(Color.RED)); //

		Plot plotSoc = Plot.plot(//
				Plot.plotOpts() //
						.title(testDescription + "- SoC") //
						.legend(Plot.LegendFormat.BOTTOM)) //
				.xAxis("x", Plot.axisOpts() //
						.format(AxisFormat.NUMBER_INT) //
						.range(0, 96)) //
				.yAxis("y", Plot.axisOpts() //
						.format(AxisFormat.NUMBER_INT) //
						.range(0, 100)) //
				.series("State of Charge", dataSoc, Plot.seriesOpts() //
						.color(Color.BLACK)); //

		Plot plotPrediction = Plot.plot(//
				Plot.plotOpts() //
						.title(testDescription) //
						.legend(Plot.LegendFormat.BOTTOM)) //
				.xAxis("x", Plot.axisOpts() //
						.format(AxisFormat.NUMBER_INT) //
						.range(0, 96)) //
				.yAxis("y", Plot.axisOpts() //
						.format(AxisFormat.NUMBER_INT)) //
				.series("Production", dataProductionPrediction, Plot.seriesOpts() //
						.color(Color.GREEN)) //
				.series("Consumption", dataConsumptionPrediction, Plot.seriesOpts() //
						.color(Color.YELLOW)); //

		try {
			if (oldLogic) {
				testDescription = "./testResults/Old/" + testDescription;
			} else {
				testDescription = "./testResults/New/" + testDescription;
			}

			plot.save(testDescription + "/logic", "png");
			plotSoc.save(testDescription + "/soc", "png");
			plotPrediction.save(testDescription + "/prediction", "png");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new DelayChargeResult(socFloat, totoalActivePower * 0.25f, totoalActivePowerLessEfficiency * 0.25f);
	}

	@Test
	public void calculateAvailEnergy_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T08:00:00.00Z"), ZoneOffset.UTC);
		final var midnight = ZonedDateTime.now(clock).truncatedTo(DAYS);
		final var cm = new DummyComponentManager(clock);
		final var sum = new DummySum();
		final var predictorManager = new DummyPredictorManager(
				// Production
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_PRODUCTION_ACTIVE_POWER, midnight, DEFAULT_PRODUCTION_PREDICTION),
						SUM_PRODUCTION_ACTIVE_POWER),
				// Consumption
				new DummyPredictor(PREDICTOR_ID, cm,
						Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, midnight, DEFAULT_CONSUMPTION_PREDICTION),
						SUM_CONSUMPTION_ACTIVE_POWER));

		var production = predictorManager.getPrediction(SUM_PRODUCTION_ACTIVE_POWER).asArray();
		var consumption = predictorManager.getPrediction(SUM_CONSUMPTION_ACTIVE_POWER).asArray();

		// Positive result 08:00 - 12:00
		int result1 = DelayCharge.calculateAvailEnergy(production, consumption, clock, 720 /* 12:00 */);

		assertEquals(6_372 /* Wh */, result1);

		// Negative result 08:00 - 08:15
		int result2 = DelayCharge.calculateAvailEnergy(production, consumption, clock, 495 /* 08:15 */);

		assertEquals(-74, result2);

		/*
		 * Now is between quarter hour
		 */
		final TimeLeapClock clockInQuarterHour = new TimeLeapClock(Instant.parse("2020-01-01T08:09:00.00Z"),
				ZoneOffset.UTC);

		// Positive result 08:09 - 12:00 (Energy is increasing because there is more
		// consumption as production at that time)
		int result1v2 = DelayCharge.calculateAvailEnergy(production, consumption, clockInQuarterHour, 720 /* 12:00 */);

		assertEquals(6417, result1v2);

		// Negative result 08:09 - 08:15
		int result2v2 = DelayCharge.calculateAvailEnergy(production, consumption, clockInQuarterHour, 495 /* 08:15 */);

		assertEquals(-29, result2v2);
	}

	@Test
	public void testPredictedChargeStart() throws OpenemsException {
		int targetMinute = ControllerEssGridOptimizedChargeImplTest.getValidTargetMinute("17:00");
		TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T08:00:00.00Z"), ZoneOffset.UTC);
		int capacity = 10_000;

		int soc = 76;
		long sutResult = DelayCharge.getPredictedChargeStart(targetMinute, capacity, soc, clock);

		var expected = LocalDate.now(clock).atTime(LocalTime.of(14, 5));
		var expectedAsDefault = ZonedDateTime.ofLocal(expected, ZoneId.systemDefault(), null);

		assertEquals(expectedAsDefault.toEpochSecond(), sutResult);

		// SoC - 32
		soc = 32;
		sutResult = DelayCharge.getPredictedChargeStart(targetMinute, capacity, soc, clock);

		expected = LocalDate.now(clock).atTime(LocalTime.of(8, 40));
		expectedAsDefault = ZonedDateTime.ofLocal(expected, ZoneId.systemDefault(), null);

		assertEquals(expectedAsDefault.toEpochSecond(), sutResult);

		// SoC - 99
		soc = 99;
		sutResult = DelayCharge.getPredictedChargeStart(targetMinute, capacity, soc, clock);

		expected = LocalDate.now(clock).atTime(LocalTime.of(16, 55));
		expectedAsDefault = ZonedDateTime.ofLocal(expected, ZoneId.systemDefault(), null);

		assertEquals(expectedAsDefault.toEpochSecond(), sutResult);

		// SoC - 10 HighCap
		soc = 10;
		capacity = 44_000;
		clock = new TimeLeapClock(Instant.parse("2020-01-01T09:00:00.00Z"), ZoneOffset.UTC);
		targetMinute = ControllerEssGridOptimizedChargeImplTest.getValidTargetMinute("16:00");

		sutResult = DelayCharge.getPredictedChargeStart(targetMinute, capacity, soc, clock);

		expected = LocalDate.now(clock).atTime(LocalTime.of(4, 55));
		expectedAsDefault = ZonedDateTime.ofLocal(expected, ZoneId.systemDefault(), null);

		assertEquals(expectedAsDefault.toEpochSecond(), sutResult);
	}

	@Test
	public void getCalculatedPowerLimit_morningTest_HighCap() throws Exception {

		/*
		 * Initial values
		 */
		TimeLeapClock clock = new TimeLeapClock(Instant.parse("2022-04-04T07:05:00.00Z"), ZoneOffset.UTC);
		final int targetMinute = ControllerEssGridOptimizedChargeImplTest.getValidTargetMinute("14:00");
		final DelayChargeRiskLevel riskLevel = DelayChargeRiskLevel.MEDIUM;
		final int maxApparentPower = 10000;
		final int soc = 10;
		final int capacity = 44_000;
		final ControllerEssGridOptimizedChargeImpl parent = new ControllerEssGridOptimizedChargeImpl();

		// Minimum charge power, to avoid low charge power
		int minimumPower = Math.round(capacity * 0.11F);

		/*
		 * Calculated values (Calculate the remaining capacity with soc minus one, to
		 * avoid very high results at the end.)
		 */
		final int remainingCapacity = Math.round(capacity * (100 - (soc - 1)) * 36);
		final int remainingTime = DelayCharge.calculateRemainingTime(clock, targetMinute);

		Integer maximumChargePower = DelayCharge.getCalculatedPowerLimit(remainingCapacity, remainingTime,
				DEFAULT_PRODUCTION_PREDICTION, DEFAULT_CONSUMPTION_PREDICTION, clock, riskLevel, maxApparentPower,
				targetMinute, minimumPower, parent);

		// Expected energy is to low ()
		// assertNull(maximumChargePower);
		if (maximumChargePower == null) {
			fail("No limit is applied");
		}

		// If Energy calculation would be applied on medium risk level - Predicted
		// available Energy is not enough to reach 100%
		assertEquals(5788, (int) maximumChargePower); //
	}

	private static int getValidTargetMinute(String manualTargetTime) {
		LocalTime targetTime = null;

		// Try to parse the configured Time as LocalTime or ZonedDateTime, which is the
		// format that comes from UI.
		targetTime = DelayCharge.parseTime(manualTargetTime);
		if (targetTime == null) {
			targetTime = LocalTime.of(17, 0);
			System.out.println("No valid target time - Default time is used");
		}

		return targetTime.get(ChronoField.MINUTE_OF_DAY);
	}
}
