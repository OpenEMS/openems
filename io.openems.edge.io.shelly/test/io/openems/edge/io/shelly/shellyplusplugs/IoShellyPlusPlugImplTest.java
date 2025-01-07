package io.openems.edge.io.shelly.shellyplusplugs;

import static io.openems.common.types.MeterType.PRODUCTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.timedata.test.DummyTimedata;

public class IoShellyPlusPlugImplTest {

	@Test
	public void test() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var sut = new IoShellyPlusPlugsImpl();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setPhase(SinglePhase.L1) //
						.setIp("127.0.0.1") //
						.setType(PRODUCTION) //
						.build()) //

				.next(new TestCase("Successful read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									  "sys": {
									    "available_updates": {
									      "foo": "bar"
									    }
									  },
									  "switch:0": {
									    "current": 1.234,
									    "voltage": 231.5,
									    "output": false,
									    "apower": 789.1
									  }
									}
									"""));
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("-|789 W", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 789) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 789) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 231500) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 231500) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null) //
						.output(ElectricityMeter.ChannelId.CURRENT, 1234) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 1234) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
						.output(IoShellyPlusPlugs.ChannelId.RELAY, null) //
						.output(IoShellyPlusPlugs.ChannelId.SLAVE_COMMUNICATION_FAILED, false)) //

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
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 0L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 0L) //
						.output(IoShellyPlusPlugs.ChannelId.RELAY, null) //
						.output(IoShellyPlusPlugs.ChannelId.SLAVE_COMMUNICATION_FAILED, true)) //

				// Test case for writing to relay
				.next(new TestCase("Write") //
						.onBeforeControllersCallbacks(() -> {
							sut.setRelay(true);
						}) //
						.also(testCase -> {
							final var relayTurnedOn = httpTestBundle
									.expect("http://127.0.0.1/rpc/Switch.Set?id=0&on=true").toBeCalled();

							testCase.onBeforeControllersCallbacks(() -> httpTestBundle.triggerNextCycle());
							testCase.onAfterWriteCallbacks(
									() -> assertTrue("Failed to turn on relay", relayTurnedOn.get()));
						})) //

				.deactivate();//
	}
}
