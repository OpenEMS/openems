package io.openems.edge.io.shelly.shellyplusplugs;

import static io.openems.common.types.MeterType.PRODUCTION;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L2;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.VOLTAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.timedata.test.DummyTimedata;

public class IoShellyPlugImplTest {

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
						.onBeforeControllersCallbacks(() -> {
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
						.output(ACTIVE_POWER, 789) //
						.output(ACTIVE_POWER_L1, 789) //
						.output(ACTIVE_POWER_L2, null) //
						.output(CURRENT, 1234) //
						.output(VOLTAGE, 231500)) //

				.next(new TestCase("Invalid read response") //
						.onBeforeControllersCallbacks(() -> assertEquals("Off|789 W", sut.debugLog()))

						.onBeforeControllersCallbacks(() -> {
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							httpTestBundle.triggerNextCycle();
						}) //
						.output(ACTIVE_POWER, null) //
						.output(ACTIVE_POWER_L1, null) //
						.output(ACTIVE_POWER_L2, null) //
						.output(CURRENT, null) //
						.output(VOLTAGE, null) //

						.output(ACTIVE_PRODUCTION_ENERGY, 0L) //
						.output(ACTIVE_CONSUMPTION_ENERGY, 0L)) //

				.next(new TestCase("Write") //
						.onBeforeControllersCallbacks(() -> assertEquals("Unknown|UNDEFINED", sut.debugLog()))
						.onBeforeControllersCallbacks(() -> {
							sut.setRelay(true);
						}) //
						.also(testCase -> {
							final var relayTurnedOn = httpTestBundle.expect("http://127.0.0.1/relay/0?turn=on")
									.toBeCalled();

							testCase.onBeforeControllersCallbacks(() -> httpTestBundle.triggerNextCycle());
							testCase.onAfterWriteCallbacks(
									() -> assertTrue("Failed to turn on relay", relayTurnedOn.get()));
						})) //

				.deactivate();
	}

}
