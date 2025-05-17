package io.openems.edge.io.shelly.shellyem;

import static io.openems.common.types.MeterType.CONSUMPTION_METERED;
import static io.openems.edge.meter.api.SinglePhase.L1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.io.shelly.shelly3em.IoShelly3Em;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.ElectricityMeter;

public class IoShellyEmTest {

	@Test
	public void test() throws Exception {
		final var sut = new IoShellyEmImpl();
		final var httpTestBundle = new DummyBridgeHttpBundle();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setPhase(L1) //
						.setIp("127.0.0.1") //
						.setType(CONSUMPTION_METERED) //
						.setSumEmeter1AndEmeter2(true) //
						.setChannel(0) //
						.build()) //

				.next(new TestCase("Successful read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
								    {
								      "relays": [
								        {
								          "ison": true,
								          "overpower": false
								        }
								      ],
								      "emeters": [
								        {
								          "power": 200,
								          "reactive": 0.00,
								          "pf": -0.08,
								          "voltage": 224.23,
								          "is_valid": true,
								          "total": 3301872.8,
								          "total_returned": 785.7
								        },
								        {
								          "power": 589,
								          "reactive": -12.07,
								          "pf": -0.39,
								          "voltage": 224.23,
								          "is_valid": true,
								          "total": 683451.3,
								          "total_returned": 194.1
								        }
								      ],
								      "update": {
								        "status": "idle",
								        "has_update": false
								      }
								    }
								"""));
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("x|789 W", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 789) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 789) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 224230) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 224230) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null) //
						.output(ElectricityMeter.ChannelId.CURRENT, 3000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 3000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
						.output(IoShellyEm.ChannelId.RELAY, null) //
						.output(IoShellyEm.ChannelId.SLAVE_COMMUNICATION_FAILED, false) //
						.output(IoShellyEm.ChannelId.HAS_UPDATE, false)) //
				
				.next(new TestCase("Invalid read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("?|UNDEFINED", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null) //
						.output(ElectricityMeter.ChannelId.CURRENT, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
						.output(IoShellyEm.ChannelId.RELAY, null) //
						.output(IoShellyEm.ChannelId.SLAVE_COMMUNICATION_FAILED, true) //
						.output(IoShellyEm.ChannelId.HAS_UPDATE, false)) //

				// Test case for writing to relay
				.next(new TestCase("Write") //
						.onBeforeControllersCallbacks(() -> {
							sut.setRelay(true);
						}) //
						.also(testCase -> {
							final var relayTurnedOn = httpTestBundle.expect("http://127.0.0.1/relay/0?turn=on")
									.toBeCalled();
							testCase.onBeforeControllersCallbacks(() -> {
								httpTestBundle.triggerNextCycle();
							});
							testCase.onAfterWriteCallbacks(() -> {
								assertTrue("Failed to turn on relay", relayTurnedOn.get());
							});
						})) //

				.deactivate();//
	}
}
