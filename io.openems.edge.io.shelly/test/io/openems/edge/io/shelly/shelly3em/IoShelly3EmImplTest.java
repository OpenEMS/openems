package io.openems.edge.io.shelly.shelly3em;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.timedata.test.DummyTimedata;

public class IoShelly3EmImplTest {

	private static final String COMPONENT_ID = "io0";

	private static final ChannelAddress RELAY_OVERPOWER = new ChannelAddress(COMPONENT_ID, "RelayOverpowerException");
	private static final ChannelAddress HAS_UPDATE = new ChannelAddress(COMPONENT_ID, "HasUpdate");
	private static final ChannelAddress ACTIVE_POWER = new ChannelAddress(COMPONENT_ID, "ActivePower");
	private static final ChannelAddress ACTIVE_POWER_L1 = new ChannelAddress(COMPONENT_ID, "ActivePowerL1");
	private static final ChannelAddress ACTIVE_POWER_L2 = new ChannelAddress(COMPONENT_ID, "ActivePowerL2");
	private static final ChannelAddress ACTIVE_POWER_L3 = new ChannelAddress(COMPONENT_ID, "ActivePowerL3");
	private static final ChannelAddress VOLTAGE_L1 = new ChannelAddress(COMPONENT_ID, "VoltageL1");
	private static final ChannelAddress VOLTAGE_L2 = new ChannelAddress(COMPONENT_ID, "VoltageL2");
	private static final ChannelAddress VOLTAGE_L3 = new ChannelAddress(COMPONENT_ID, "VoltageL3");
	private static final ChannelAddress CURRENT_L1 = new ChannelAddress(COMPONENT_ID, "CurrentL1");
	private static final ChannelAddress CURRENT_L2 = new ChannelAddress(COMPONENT_ID, "CurrentL2");
	private static final ChannelAddress CURRENT_L3 = new ChannelAddress(COMPONENT_ID, "CurrentL3");
	private static final ChannelAddress EMETER1_EXCEPTION = new ChannelAddress(COMPONENT_ID, "Emeter1Exception");
	private static final ChannelAddress EMETER2_EXCEPTION = new ChannelAddress(COMPONENT_ID, "Emeter2Exception");
	private static final ChannelAddress EMETER3_EXCEPTION = new ChannelAddress(COMPONENT_ID, "Emeter3Exception");
	private static final ChannelAddress SLAVE_COMMUNICATION_FAILED = new ChannelAddress(COMPONENT_ID,
			"SlaveCommunicationFailed");
	private static final ChannelAddress PRODUCTION_ENERGY = new ChannelAddress(COMPONENT_ID, "ActiveProductionEnergy");
	private static final ChannelAddress CONSUMPTION_ENERGY = new ChannelAddress(COMPONENT_ID,
			"ActiveConsumptionEnergy");

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoShelly3EmImpl()) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofDummyBridge()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("127.0.0.1") //
						.setType(MeterType.GRID) //
						.build()) //

				.next(new TestCase("Successful read response")
						.onBeforeControllersCallbacks(() -> bridge.mockCycleResult("""
										{
										  "relays": [
										    {
										      "ison": true,
										      "overpower": false
										    }
										  ],
										  "emeters": [
										    {
										      "power": 8.52,
										      "current": 1,
										      "voltage": 230,
										      "is_valid": true
										    },
										    {
										      "power": 31.39,
										      "current": 2,
										      "voltage": 231,
										      "is_valid": true
										    },
										    {
										      "power": 58.75,
										      "current": 3,
										      "voltage": 232,
										      "is_valid": false
										    }
										  ],
										  "total_power": 35.88,
										  "emeter_n": {
										    "current": 0,
										    "ixsum": 0.7,
										    "mismatch": false,
										    "is_valid": false
										  },
										  "update": {
										    "status": "idle",
										    "has_update": false
										  }
										}
								""")) //
						.output(RELAY_OVERPOWER, false) //
						.output(HAS_UPDATE, false) //
						.output(ACTIVE_POWER, 99) //
						.output(ACTIVE_POWER_L1, 9) //
						.output(ACTIVE_POWER_L2, 31) //
						.output(ACTIVE_POWER_L3, 59) //
						.output(VOLTAGE_L1, 230000) //
						.output(VOLTAGE_L2, 231000) //
						.output(VOLTAGE_L3, 232000) //
						.output(CURRENT_L1, 1000) //
						.output(CURRENT_L2, 2000) //
						.output(CURRENT_L3, 3000) //
						.output(EMETER1_EXCEPTION, false) //
						.output(EMETER2_EXCEPTION, false) //
						.output(EMETER3_EXCEPTION, true)) //

				.next(new TestCase("Invalid read response")
						.onBeforeControllersCallbacks(() -> assertEquals("x|99 W", sut.debugLog()))

						.onBeforeControllersCallbacks(() -> bridge.mockCycleResult(null)) //
						.output(RELAY_OVERPOWER, false) //
						.output(HAS_UPDATE, false) //
						.output(ACTIVE_POWER, null) //
						.output(ACTIVE_POWER_L1, null) //
						.output(ACTIVE_POWER_L2, null) //
						.output(ACTIVE_POWER_L3, null) //
						.output(VOLTAGE_L1, null) //
						.output(VOLTAGE_L2, null) //
						.output(VOLTAGE_L3, null) //
						.output(CURRENT_L1, null) //
						.output(CURRENT_L2, null) //
						.output(CURRENT_L3, null) //
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