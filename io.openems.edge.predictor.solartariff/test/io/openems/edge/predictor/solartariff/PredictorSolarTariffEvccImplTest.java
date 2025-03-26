package io.openems.edge.predictor.solartariff;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;

public class PredictorSolarTariffEvccImplTest {

	private static final ChannelAddress METER1_ACTIVE_POWER = new ChannelAddress("meter1", "ActivePower");

	@Test
	public void test() throws Exception {
		
		//TODO not yet implemented
		
/*
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800)  starts at 1. January 2020 00:00:00 ,
				ZoneOffset.UTC);

		var values = Data.data;
		var predictedValues = Data.predictedData;

		var timedata = new DummyTimedata("timedata0");
		var start = ZonedDateTime.of(2019, 12, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

		for (var i = 0; i < values.length; i++) {
			timedata.add(start.plusMinutes(i * 15), METER1_ACTIVE_POWER, values[i]);
		}

		var sut = new PredictorSolarTariffEvccImplTest();

		new ComponentTest(sut) //
				.addReference("timedata", timedata) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("predictor0") //
						.setUrl("http://localhost:7070/api/tariff/solar") //
						.setChannelAddresses(METER1_ACTIVE_POWER.toString()) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build());

		var prediction = sut.getPrediction(METER1_ACTIVE_POWER);
		var p = prediction.asArray();

		assertEquals(predictedValues[0], p[0]);
		assertEquals(predictedValues[48], p[48]);
		assertEquals(predictedValues[95], p[95]);*/
	}

}
