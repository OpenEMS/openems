package io.openems.edge.predictor.lstmmodel;

import static org.junit.Assert.*;

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

public class LstmModelPredictorTest {

	private static final String TIMEDATA_ID = "timedata0";
	private static final String PREDICTOR_ID = "predictor0";

	private static final ChannelAddress METER1_ACTIVE_POWER = new ChannelAddress("meter1", "ActivePower");

	@Test
	public void test() throws Exception {

		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);

		var values = Data.data;
		System.out.println(values.length);
		var predictedValues = Data.predictedData;

		var timedata = new DummyTimedata(TIMEDATA_ID);
		var start = ZonedDateTime.of(2019, 12, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

		for (var i = 0; i < values.length; i++) {
			timedata.add(start.plusMinutes(i * 15), METER1_ACTIVE_POWER, values[i]);
		}
		
		

		var sut = new LstmPredictorImpl();

		new ComponentTest(sut) //
				.addReference("timedata", timedata) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId(PREDICTOR_ID) //
						.setNumOfWeeks(4) //
						.setChannelAddresses(METER1_ACTIVE_POWER.toString()).build());

		var prediction = sut.get24HoursPrediction(METER1_ACTIVE_POWER);
//		var p = prediction.getValues();

//		assertEquals(predictedValues[0], p[0]);
//		assertEquals(predictedValues[48], p[48]);
//		assertEquals(predictedValues[95], p[95]);

//		System.out.println(Arrays.toString(prediction.getValues()));

	}

}

