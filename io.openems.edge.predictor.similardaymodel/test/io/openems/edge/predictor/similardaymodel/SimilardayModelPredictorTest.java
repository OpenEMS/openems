package io.openems.edge.predictor.similardaymodel;

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
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.timedata.test.DummyTimedata;

public class SimilardayModelPredictorTest {

	private static final String TIMEDATA_ID = "timedata0";
	private static final String PREDICTOR_ID = "predictor0";

	private static final ChannelAddress METER1_ACTIVE_POWER = new ChannelAddress("meter1", "ActivePower");

	@Test
	public void test() throws Exception {

		final TimeLeapClock clock = new TimeLeapClock(
				Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);

		Integer[] values = Data.data;
		Integer[] predictedValues = Data.predictedData;

		DummyTimedata timedata = new DummyTimedata(TIMEDATA_ID);
		ZonedDateTime start = ZonedDateTime.of(2019, 12, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

		for (int i = 0; i < values.length; i++) {
			timedata.add(start.plusMinutes(i * 15), METER1_ACTIVE_POWER, values[i]);
		}

		SimilarDayPredictorImpl sut = new SimilarDayPredictorImpl();

		new ComponentTest(sut) //
				.addReference("timedata", timedata) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId(PREDICTOR_ID) //
						.setNumOfWeeks(4) //
						.setChannelAddresses(METER1_ACTIVE_POWER.toString()).build());

		Prediction24Hours prediction = sut.get24HoursPrediction(METER1_ACTIVE_POWER);
		Integer[] p = prediction.getValues();

		assertEquals(predictedValues[0], p[0]);
		assertEquals(predictedValues[48], p[48]);
		assertEquals(predictedValues[95], p[95]);

		System.out.println(Arrays.toString(prediction.getValues()));

	}

}
