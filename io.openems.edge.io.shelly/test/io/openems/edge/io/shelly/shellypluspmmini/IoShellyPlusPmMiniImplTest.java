package io.openems.edge.io.shelly.shellypluspmmini;

import static io.openems.common.types.MeterType.CONSUMPTION_METERED;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.test.DummyTimedata;

public class IoShellyPlusPmMiniImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new IoShellyPlusPmMiniImpl();
		final var httpTestBundle = new DummyBridgeHttpBundle();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIp("127.0.0.1") //
						.setType(CONSUMPTION_METERED) //
						.setPhase(SinglePhase.L1) //
						.setInvert(false) //
						.build()) //

				.next(new TestCase("Successful read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									  "pm1:0":{
									    "id":0,
									    "voltage":237.2,
									    "current":0.106,
									    "apower":6.7,
									    "freq":50.1,
									    "aenergy":{
									      "total":73284.398,
									      "by_minute":[
									        219.104,
									        0,
									        219.104
									      ],
									      "minute_ts":1751037360
									    },
									    "ret_aenergy":{
									      "total":0,
									      "by_minute":[
									        0,
									        0,
									        0
									      ],
									      "minute_ts":1751037360
									    }
									  }
									}"""));
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("L1:7 W", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 7) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 7) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 237200) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 237200) //
						.output(ElectricityMeter.ChannelId.CURRENT, 106) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 106) //
						.output(IoShellyPlusPmMini.ChannelId.SLAVE_COMMUNICATION_FAILED, false)) //

				.next(new TestCase("Invalid read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("L1:UNDEFINED", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, null) //
						.output(ElectricityMeter.ChannelId.CURRENT, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, null) //
						.output(IoShellyPlusPmMini.ChannelId.SLAVE_COMMUNICATION_FAILED, true)) //

				.deactivate();
	}

	@Test
	public void testInvert() throws Exception {
		final var sut = new IoShellyPlusPmMiniImpl();
		final var httpTestBundle = new DummyBridgeHttpBundle();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIp("127.0.0.1") //
						.setType(CONSUMPTION_METERED) //
						.setPhase(SinglePhase.L1) //
						.setInvert(true) //
						.build()) //

				.next(new TestCase("Successful read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									  "pm1:0":{
									    "id":0,
									    "voltage":237.2,
									    "current":0.106,
									    "apower":6.7,
									    "freq":50.1,
									    "aenergy":{
									      "total":73284.398,
									      "by_minute":[
									        219.104,
									        0,
									        219.104
									      ],
									      "minute_ts":1751037360
									    },
									    "ret_aenergy":{
									      "total":0,
									      "by_minute":[
									        0,
									        0,
									        0
									      ],
									      "minute_ts":1751037360
									    }
									  }
									}"""));
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("L1:-7 W", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, -7) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, -7) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 237200) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 237200) //
						.output(ElectricityMeter.ChannelId.CURRENT, -106) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, -106) //
						.output(IoShellyPlusPmMini.ChannelId.SLAVE_COMMUNICATION_FAILED, false));
	}
}