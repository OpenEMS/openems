package io.openems.edge.predictor.persistencemodel;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.timedata.test.DummyTimedata;

public class PredictorPersistenceModelImplTest {

	private static final String TIMEDATA_ID = "timedata0";
	private static final String PREDICTOR_ID = "predictor0";

	private static final ChannelAddress METER1_ACTIVE_POWER = new ChannelAddress("meter1", "ActivePower");

	@Test
	public void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);
		int[] values = {
				// Day 1
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 9, 146, 348, 636, 1192, 2092, 2882, 3181,
				3850, 5169, 6005, 6710, 7372, 8138, 8918, 9736, 10615, 11281, 11898, 12435, 11982, 14287, 15568, 16747,
				16934, 17221, 17573, 15065, 16726, 16670, 16696, 16477, 16750, 16991, 17132, 17567, 17003, 17686, 17753,
				17773, 17381, 17059, 17110, 16395, 15803, 15044, 14413, 13075, 12975, 6748, 7845, 10781, 8605, 6202,
				3049, 1697, 1184, 1142, 1015, 568, 1093, 414, 121, 110, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				// Day 2
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 6, 146, 297, 489, 1111, 1953, 3825,
				2346, 3356, 3407, 3482, 4238, 7179, 11642, 5486, 4265, 5488, 5559, 6589, 7608, 9285, 7668, 6077, 3918,
				4498, 7221, 9628, 11962, 9483, 11746, 10401, 8875, 8825, 13945, 16488, 13038, 17702, 16772, 7319, 228,
				477, 501, 547, 589, 1067, 13304, 17367, 14825, 13654, 12545, 8371, 10468, 9810, 8537, 6228, 3758, 4131,
				3572, 1698, 1017, 569, 188, 14, 2, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		var timedata = new DummyTimedata(TIMEDATA_ID);
		var start = ZonedDateTime.of(2019, 12, 30, 0, 0, 0, 0, ZoneId.of("UTC"));
		for (var i = 0; i < values.length; i++) {
			timedata.add(start.plusMinutes(i * 15), METER1_ACTIVE_POWER, values[i]);
		}

		var sut = new PredictorPersistenceModelImpl();

		new ComponentTest(sut) //
				.addReference("timedata", timedata) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId(PREDICTOR_ID) //
						.setChannelAddresses(METER1_ACTIVE_POWER.toString()) //
						.build());

		var prediction = sut.get24HoursPrediction(METER1_ACTIVE_POWER);
		var p = prediction.getValues();

		assertEquals((Integer) 0, p[0]);
		assertEquals((Integer) 3, p[20]);
		assertEquals((Integer) 6, p[21]);
		assertEquals((Integer) 146, p[22]);
		assertEquals((Integer) 297, p[23]);

		System.out.println(Arrays.toString(prediction.getValues()));
	}

}
