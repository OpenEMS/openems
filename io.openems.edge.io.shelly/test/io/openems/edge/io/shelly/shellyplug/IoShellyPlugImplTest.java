package io.openems.edge.io.shelly.shellyplug;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;

public class IoShellyPlugImplTest {

	private static final String COMPONENT_ID = "io0";

	private static final ChannelAddress ACTIVE_POWER = new ChannelAddress(COMPONENT_ID, "ActivePower");
	private static final ChannelAddress RELAY = new ChannelAddress(COMPONENT_ID, "Relay");
	private static final ChannelAddress PRODUCTION_ENERGY = new ChannelAddress(COMPONENT_ID, "ActiveProductionEnergy");
	private static final ChannelAddress CONSUMPTION_ENERGY = new ChannelAddress(COMPONENT_ID,
			"ActiveConsumptionEnergy");
	private static final ChannelAddress SLAVE_COMMUNICATION_FAILED = new ChannelAddress(COMPONENT_ID,
			"SlaveCommunicationFailed");

	@Test
	public void test() throws Exception {
		final var bridgeFactory = new DummyBridgeHttpFactory();
		final var bridge = bridgeFactory.bridge;
		final var sut = new IoShellyPlugImpl();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofDummyBridge()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setPhase(SinglePhase.L1) //
						.setIp("127.0.0.1") //
						.setType(MeterType.PRODUCTION) //
						.build()) //

				.next(new TestCase("Successful read response") //
						.onBeforeControllersCallbacks(() -> bridge.mockCycleResult("""
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
								""")) //
						.output(ACTIVE_POWER, 789) //
						// .output(RELAY, true) // TODO Also Test the Relay State
						.output(PRODUCTION_ENERGY, 1200L)) //

				.next(new TestCase("Invalid read response") //
						.onBeforeControllersCallbacks(() -> assertEquals("x|789 W", sut.debugLog()))

						.onBeforeControllersCallbacks(() -> bridge.mockCycleResult("")) //
						.output(ACTIVE_POWER, null) //
						.output(RELAY, null) //
						.output(PRODUCTION_ENERGY, 0L) //
						.output(CONSUMPTION_ENERGY, 0L) //
						.output(SLAVE_COMMUNICATION_FAILED, true)) //

				.next(new TestCase("Write") //
						.onBeforeControllersCallbacks(() -> assertEquals("?|UNDEFINED", sut.debugLog()))

						.onBeforeControllersCallbacks(() -> {
							sut.setRelay(true);
							bridge.mockRequestResult("FOO-BAR");
						})) //

				.deactivate();
	}
}