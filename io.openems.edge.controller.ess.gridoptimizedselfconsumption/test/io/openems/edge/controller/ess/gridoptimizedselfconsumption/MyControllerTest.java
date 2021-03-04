package io.openems.edge.controller.ess.gridoptimizedselfconsumption;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.test.DummyPredictor24Hours;
import io.openems.edge.predictor.api.test.DummyPredictorManager;

public class MyControllerTest {

	private static final String CTRL_ID = "ctrlGridOptimizedSelfConsumption0";

	private static final String PREDICTOR_ID = "predictor0";

	private static final String ESS_ID = "ess0";
	private static final String METER_ID = "meter0";

	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "ActivePower");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerEquals");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerLessOrEquals");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_GREATER_OR_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerGreaterOrEquals");

	private static final ChannelAddress METER_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");
//	private static final ChannelAddress METER_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");

	@Test
	public void test() throws Exception {
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final DummyComponentManager cm = new DummyComponentManager(clock);
		final Prediction24Hours productionPrediction = new Prediction24Hours(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 74, 297, 610, 913, 1399, 1838, 2261, 2662, 3052, 3405, 3708, 4011,
				4270, 4458, 4630, 4794, 4908, 4963, 4960, 4973, 4940, 4859, 4807, 4698, 4530, 4348, 4147, 3527, 3141,
				2917, 2703, 2484, 2233, 1971, 1674, 1386, 1089, 811, 531, 298, 143, 41, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 15, 130, 402, 667, 1023, 1631, 2020, 2420, 2834, 3237, 3638, 4006, 4338, 4597,
				4825, 4965, 5111, 5213, 5268, 5317, 5321, 5271, 5232, 5193, 5044, 4915, 4738, 4499, 3702, 3226, 3046,
				2857, 2649, 2421, 2184, 1933, 1674, 1364, 1070, 754, 447, 193, 52, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		final DummyPredictor24Hours productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, productionPrediction,
				"_sum/ProductionActivePower");
		final Prediction24Hours consumptionPrediction = new Prediction24Hours(1021, 1208, 713, 931, 2847, 2551, 1558,
				1234, 433, 633, 1355, 606, 430, 1432, 1121, 502, 294, 1048, 1194, 914, 1534, 1226, 1235, 977, 578, 1253,
				1983, 1417, 513, 929, 1102, 445, 1208, 2791, 2729, 2609, 2086, 1454, 848, 816, 2610, 3150, 2036, 1180,
				359, 1316, 3447, 2104, 905, 802, 828, 812, 863, 633, 293, 379, 296, 296, 436, 140, 135, 196, 230, 175,
				365, 758, 325, 264, 181, 167, 228, 1082, 777, 417, 798, 1268, 409, 830, 1191, 417, 1087, 2958, 2946,
				2235, 1343, 483, 796, 1201, 567, 395, 989, 1066, 370, 989, 1255, 660, 349, 880, 1186, 580, 327, 911,
				1135, 553, 265, 938, 1165, 567, 278, 863, 1239, 658, 236, 816, 1173, 1131, 498, 550, 1344, 1226, 874,
				504, 1733, 1809, 1576, 369, 771, 2583, 3202, 2174, 1878, 2132, 2109, 1895, 1565, 1477, 1613, 1716, 1867,
				1726, 1700, 1787, 1755, 1734, 1380, 691, 338, 168, 199, 448, 662, 205, 183, 70, 169, 276, 149, 76, 195,
				168, 159, 266, 135, 120, 224, 979, 2965, 1337, 1116, 795, 334, 390, 433, 369, 762, 2908, 3226, 2358,
				1778, 1002, 455, 654, 534, 1587, 1638, 459, 330, 258, 368, 728, 1096);
		final DummyPredictor24Hours consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm,
				consumptionPrediction, "_sum/ConsumptionActivePower");
		final DummyPredictorManager predictorManager = new DummyPredictorManager(productionPredictor,
				consumptionPredictor);
		new ControllerTest(new GridOptimizedSelfConsumptionImpl()) //
				.addReference("predictorManager", predictorManager); //
//				.activate(MyConfig.create() //
//						.setId(CTRL_ID) //
//						.build())
//				.next(new TestCase());

		System.out.println(Arrays.toString(predictorManager
				.get24HoursPrediction(ChannelAddress.fromString("_sum/ProductionActivePower")).getValues()));
	}

}
