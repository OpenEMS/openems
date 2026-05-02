package io.openems.edge.meter.opendtu;

import static io.openems.common.types.MeterType.PRODUCTION;
import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.channel.Level;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.bridge.http.cycle.dummy.DummyCycleSubscriber;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.timedata.test.DummyTimedata;

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
						.setSerialNumber("1234567890") //
						.build()) //
				.next(new TestCase("Successful read response") //
						.activateStrictMode() //
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
						.output(OpenemsComponent.ChannelId.STATE, Level.OK) //

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 123) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 228200) //
						.output(ElectricityMeter.ChannelId.CURRENT, 1000) //

						.output(ElectricityMeter.ChannelId.CURRENT_L1, 1000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 228200) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null) //
						.output(ElectricityMeter.ChannelId.FREQUENCY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 123) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null) //

						.output(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, null) //
						.output(ManagedSymmetricPvInverter.ChannelId.MAX_ACTIVE_POWER, null) //
						.output(ManagedSymmetricPvInverter.ChannelId.MAX_APPARENT_POWER, null) //
						.output(ManagedSymmetricPvInverter.ChannelId.MAX_REACTIVE_POWER, null) //

						.output(MeterOpenDtu.ChannelId.SLAVE_COMMUNICATION_FAILED, false)) //
				.deactivate();
	}
}
