package io.openems.edge.core.predictormanager;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;
import io.openems.edge.predictor.api.test.DummyPrediction24Hours;
import io.openems.edge.predictor.api.test.DummyPredictor24Hours;

public class PredictorManagerImplTest {

	private static final String PREDICTOR_ID = "predictor0";
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
	public void test() throws OpenemsException, Exception {
		var clock = new TimeLeapClock(Instant.parse("2020-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		var componentManager = new DummyComponentManager(clock);
		var consumptionPrediction = new DummyPrediction24Hours(DEFAULT_CONSUMPTION_PREDICTION);
		var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, componentManager, consumptionPrediction,
				"_sum/ConsumptionActivePower");

		var sut = new PredictorManagerImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", componentManager) //
				.addReference("predictors", List.<Predictor24Hours>of(consumptionPredictor)) //
				.activate(MyConfig.create()//
						.build());

		assertEquals(Prediction24Hours.EMPTY, sut.get24HoursPrediction(new ChannelAddress("_sum", "FooBar")));

		assertArrayEquals(//
				// First 96 elements only
				Stream.of(DEFAULT_CONSUMPTION_PREDICTION).limit(96).toArray(Integer[]::new),
				sut.get24HoursPrediction(new ChannelAddress("_sum", "UnmanagedConsumptionActivePower")).getValues());
	}

}
