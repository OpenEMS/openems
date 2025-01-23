package io.openems.edge.predictor.similardaymodel;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.predictor.api.prediction.LogVerbosity;
import io.openems.edge.timedata.test.DummyTimedata;

public class PredictorSimilardayModelImplTest {

	private static final ChannelAddress METER1_ACTIVE_POWER = new ChannelAddress("meter1", "ActivePower");

	@Test
	public void test() throws Exception {

		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);

		var values = Data.data;
		var predictedValues = Data.predictedData;

		var timedata = new DummyTimedata("timedata0");
		var start = ZonedDateTime.of(2019, 12, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

		for (var i = 0; i < values.length; i++) {
			timedata.add(start.plusMinutes(i * 15), METER1_ACTIVE_POWER, values[i]);
		}

		var sut = new PredictorSimilardayModelImpl();

		new ComponentTest(sut) //
				.addReference("timedata", timedata) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("predictor0") //
						.setNumOfWeeks(4) //
						.setChannelAddresses(METER1_ACTIVE_POWER.toString()) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build());

		var prediction = sut.getPrediction(METER1_ACTIVE_POWER);
		var p = prediction.asArray();

		assertEquals(predictedValues[0], p[0]);
		assertEquals(predictedValues[48], p[48]);
		assertEquals(predictedValues[95], p[95]);
	}

}
