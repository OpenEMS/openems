package io.openems.edge.io.shelly.shellyplusplugs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.timedata.test.DummyTimedata;

public class IoShellyPlusPlugImplTest {

	private static final String COMPONENT_ID = "io0";

	private static final ChannelAddress ACTIVE_POWER = new ChannelAddress(COMPONENT_ID, "ActivePower");
	private static final ChannelAddress ACTIVE_POWER_L1 = new ChannelAddress(COMPONENT_ID, "ActivePowerL1");
	private static final ChannelAddress ACTIVE_POWER_L2 = new ChannelAddress(COMPONENT_ID, "ActivePowerL2");
	private static final ChannelAddress CURRENT = new ChannelAddress(COMPONENT_ID, "Current");
	private static final ChannelAddress VOLTAGE = new ChannelAddress(COMPONENT_ID, "Voltage");
	private static final ChannelAddress PRODUCTION_ENERGY = new ChannelAddress(COMPONENT_ID, "ActiveProductionEnergy");
	private static final ChannelAddress CONSUMPTION_ENERGY = new ChannelAddress(COMPONENT_ID,
			"ActiveConsumptionEnergy");

	@Test
	public void test() throws Exception {
		final var bridgeFactory = new DummyBridgeHttpFactory();
		final var bridge = bridgeFactory.bridge;
		final var sut = new IoShellyPlusPlugsImpl();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", bridgeFactory) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setPhase(SinglePhase.L1) //
						.setIp("127.0.0.1") //
						.setType(MeterType.PRODUCTION) //
						.build()) //

				.next(new TestCase("Successfull read response") //
						.onBeforeControllersCallbacks(() -> bridge.mockCycleResult("""
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
								""")) //
						.output(ACTIVE_POWER, 789) //
						.output(ACTIVE_POWER_L1, 789) //
						.output(ACTIVE_POWER_L2, null) //
						.output(CURRENT, 1234) //
						.output(VOLTAGE, 231500)) //

				.next(new TestCase("Invalid read response") //
						.onBeforeControllersCallbacks(() -> assertEquals("-|789 W", sut.debugLog()))

						.onBeforeControllersCallbacks(() -> bridge.mockCycleResult("failed")) //
						.output(ACTIVE_POWER, null) //
						.output(ACTIVE_POWER_L1, null) //
						.output(ACTIVE_POWER_L2, null) //
						.output(CURRENT, null) //
						.output(VOLTAGE, null) //

						.output(PRODUCTION_ENERGY, 0L) //
						.output(CONSUMPTION_ENERGY, 0L)) //

				.next(new TestCase("Write") //
						.onBeforeControllersCallbacks(() -> assertEquals("?|UNDEFINED", sut.debugLog()))

						.onBeforeControllersCallbacks(() -> {
							sut.setRelay(true);
							bridge.mockRequestResult("FOO-BAR");
						})) //

				.deactivate();
	}
}
