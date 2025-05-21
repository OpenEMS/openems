package io.openems.edge.io.shelly.shellyplug;

import static io.openems.common.types.MeterType.PRODUCTION;
import static io.openems.edge.meter.api.SinglePhase.L1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.ElectricityMeter;

public class IoShellyPlugImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new IoShellyPlugImpl();
		final var httpTestBundle = new DummyBridgeHttpBundle();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setPhase(L1) //
						.setIp("127.0.0.1") //
						.setType(PRODUCTION) //
						.build()) //

				.next(new TestCase("Successful read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									  "relays": [
									    {
									      "ison": true
									    }
									  ],
									  "meters": [
									    {
									      "power": 789.1,
									      "total": 72000
									    }
									  ]
									}
									"""));
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("x|789 W", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 789) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 789) //
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
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 1200L) //
						.output(IoShellyPlug.ChannelId.RELAY, null) //
						.output(IoShellyPlug.ChannelId.SLAVE_COMMUNICATION_FAILED, false)) //

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
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 1200L) //
						.output(IoShellyPlug.ChannelId.RELAY, null) //
						.output(IoShellyPlug.ChannelId.SLAVE_COMMUNICATION_FAILED, true)) //

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
