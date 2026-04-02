package io.openems.edge.meter.opendtu;

import static io.openems.common.types.MeterType.PRODUCTION;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.bridge.http.cycle.dummy.DummyCycleSubscriber;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;


public class MeterOpenDtuImplTest {

	@Test
	public void test() throws Exception {
		final var odtu = new MeterOpenDtuImpl();
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var dummyCycleSubscriber = new DummyCycleSubscriber();
		new ComponentTest(odtu) //
		.addReference("httpBridgeFactory", httpTestBundle.factory()) //
		.addReference("httpBridgeCycleServiceDefinition",
				new HttpBridgeCycleServiceDefinition(dummyCycleSubscriber)) //
		.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId("meterOpenDTU0") //
						.setIp("127.0.0.1") //
						.setPhase(SinglePhase.L1) //
						.setType(PRODUCTION) //
						.setSerialNumber("1234567890")

						.build()) //
				.next(new TestCase("Successful read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
								{									
								   "inverters":[
								      {
								         "AC":{
								            "0":{
								               "Power":{
								                  "v":123,
								                  "u":"W",
								                  "d":1
								               },
								               "Voltage":{
								                  "v":228.2,
								                  "u":"V",
								                  "d":1
								               },
								               "Current":{
								                  "v":1,
								                  "u":"A",
								                  "d":2
								               },
								               "Frequency":{
								                  "v":49.98,
								                  "u":"Hz",
								                  "d":2
								               },
								               "PowerFactor":{
								                  "v":0,
								                  "u":"",
								                  "d":3
								               },
								               "ReactivePower":{
								                  "v":0,
								                  "u":"var",
								                  "d":1
								               }
								            }
								         }
								      }
								   ]
								}
									"""));
							dummyCycleSubscriber.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("L1:123 W", odtu.debugLog()))
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 123) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 228200) //
						.output(ElectricityMeter.ChannelId.CURRENT, 1000) //
)
				.deactivate();
	}
}
