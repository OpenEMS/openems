package io.openems.edge.io.shelly.shelly3em;

import static io.openems.common.types.MeterType.CONSUMPTION_METERED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.io.shelly.common.ShellyCommon;
import io.openems.edge.meter.api.ElectricityMeter;

public class IoShelly3EmImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new IoShelly3EmImpl();
		final var httpTestBundle = new DummyBridgeHttpBundle();

		// Pre-set the response for the /shelly endpoint that will be called during
		// activation
		httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
					{
						"name": "shelly3em-test",
						"id": "shelly3em-12345",
						"mac": "AA:BB:CC:DD:EE:FF",
						"model": "SHEM-3",
						"gen": 1,
						"fw_id": "20230912-114516/v1.14.0-gcb84623",
						"ver": "1.14.0",
						"app": "shelly3em",
						"auth_en": false,
						"auth_domain": "shelly3em-12345"
					}
				"""));

		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIp("127.0.0.1") //
						.setType(CONSUMPTION_METERED) //
						.setInvert(false) //
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
									"""));
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("x|99 W", sut.debugLog())) //

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 99) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 9) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 31) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 59) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 231000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 231000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 232000) //
						.output(ElectricityMeter.ChannelId.CURRENT, 6000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 1000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 2000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 3000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
						.output(IoShelly3Em.ChannelId.RELAY_OVERPOWER_EXCEPTION, false) //
						.output(IoShelly3Em.ChannelId.HAS_UPDATE, false) //
						.output(IoShelly3Em.ChannelId.EMETER1_EXCEPTION, false) //
						.output(IoShelly3Em.ChannelId.EMETER2_EXCEPTION, false) //
						.output(IoShelly3Em.ChannelId.EMETER3_EXCEPTION, true) //
						.output(IoShelly3Em.ChannelId.SLAVE_COMMUNICATION_FAILED, false) //
						.output(ShellyCommon.ChannelId.AUTH_ENABLED_WARNING, false) //
						.output(ShellyCommon.ChannelId.DEVICE_GENERATION, "1")) //

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
						.output(IoShelly3Em.ChannelId.RELAY_OVERPOWER_EXCEPTION, false) //
						.output(IoShelly3Em.ChannelId.HAS_UPDATE, false) //
						.output(IoShelly3Em.ChannelId.EMETER1_EXCEPTION, false) //
						.output(IoShelly3Em.ChannelId.EMETER2_EXCEPTION, false) //
						.output(IoShelly3Em.ChannelId.EMETER3_EXCEPTION, true) //
						.output(IoShelly3Em.ChannelId.SLAVE_COMMUNICATION_FAILED, true) //
						.output(ShellyCommon.ChannelId.AUTH_ENABLED_WARNING, false) //
						.output(ShellyCommon.ChannelId.DEVICE_GENERATION, "1")) //

				// Test case for writing to relay
				.next(new TestCase("Write") //
						.onBeforeControllersCallbacks(() -> {
							sut.setRelay(true);
						}) //
						.also(testCase -> {
							final var relayTurnedOn = httpTestBundle
									.expect("http://127.0.0.1/rpc/Switch.Set?id=0&on=true").toBeCalled();

							testCase.onBeforeControllersCallbacks(() -> {
								httpTestBundle.triggerNextCycle();
							});
							testCase.onAfterWriteCallbacks(() -> {
								assertTrue("Failed to turn on relay", relayTurnedOn.get());
							});
						})) //

				.deactivate();
	}

	@Test
	public void testInvert() throws Exception {
		final var sut = new IoShelly3EmImpl();
		final var httpTestBundle = new DummyBridgeHttpBundle();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIp("127.0.0.1") //
						.setType(CONSUMPTION_METERED) //
						.setInvert(true) //
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
									"""));
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("x|-99 W", sut.debugLog())) //

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, -99) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, -9) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, -31) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, -59) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 231000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 231000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 232000) //
						.output(ElectricityMeter.ChannelId.CURRENT, -6000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, -1000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, -2000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, -3000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
						.output(IoShelly3Em.ChannelId.RELAY_OVERPOWER_EXCEPTION, false) //
						.output(IoShelly3Em.ChannelId.HAS_UPDATE, false) //
						.output(IoShelly3Em.ChannelId.EMETER1_EXCEPTION, false) //
						.output(IoShelly3Em.ChannelId.EMETER2_EXCEPTION, false) //
						.output(IoShelly3Em.ChannelId.EMETER3_EXCEPTION, true) //
						.output(IoShelly3Em.ChannelId.SLAVE_COMMUNICATION_FAILED, false));
	}

	@Test
	public void testAuthenticationWarning() throws Exception {
		final var sut = new IoShelly3EmImpl();
		final var httpTestBundle = new DummyBridgeHttpBundle();

		// Pre-set the response for the /shelly endpoint with authentication enabled
		httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
					{
						"name": "shelly3em-test",
						"id": "shelly3em-12345",
						"mac": "AA:BB:CC:DD:EE:FF",
						"model": "SHEM-3",
						"gen": 1,
						"fw_id": "20230912-114516/v1.14.0-gcb84623",
						"ver": "1.14.0",
						"app": "shelly3em",
						"auth_en": true,
						"auth_domain": "shelly3em-12345"
					}
				"""));

		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIp("127.0.0.1") //
						.setType(CONSUMPTION_METERED) //
						.build()) //
				.next(new TestCase("Authentication enabled warning") //
						.output(ShellyCommon.ChannelId.AUTH_ENABLED_WARNING, true) //
						.output(ShellyCommon.ChannelId.DEVICE_GENERATION, "1")) //
				.deactivate();
	}
}
