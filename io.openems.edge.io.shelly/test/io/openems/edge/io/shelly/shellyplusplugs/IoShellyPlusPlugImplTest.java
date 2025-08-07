package io.openems.edge.io.shelly.shellyplusplugs;

import static io.openems.common.types.MeterType.PRODUCTION;
import static io.openems.edge.common.type.Phase.SinglePhase.L1;
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
import io.openems.edge.timedata.test.DummyTimedata;

public class IoShellyPlusPlugImplTest {

	@Test
	public void test() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var sut = new IoShellyPlusPlugsImpl();
		
		// Pre-set the response for the /shelly endpoint that will be called during activation
		httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
			{
				"name": "shellyplusplugs-test",
				"id": "shellyplusplugs-12345",
				"mac": "AA:BB:CC:DD:EE:FF",
				"model": "SNSW-001P16EU",
				"gen": 2,
				"fw_id": "20230912-114516/v1.14.0-gcb84623",
				"ver": "1.14.0",
				"app": "PlusPlugS",
				"auth_en": false,
				"auth_domain": "shellyplusplugs-12345"
			}
		"""));
		
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
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
						.output(IoShellyPlusPlugs.ChannelId.SLAVE_COMMUNICATION_FAILED, false) //
						.output(ShellyCommon.ChannelId.AUTH_ENABLED_WARNING, false) //
						.output(ShellyCommon.ChannelId.DEVICE_GENERATION, "2")) //

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
						.output(IoShellyPlusPlugs.ChannelId.SLAVE_COMMUNICATION_FAILED, true) //
						.output(ShellyCommon.ChannelId.AUTH_ENABLED_WARNING, false) //
						.output(ShellyCommon.ChannelId.DEVICE_GENERATION, "2")) //

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
	
	@Test
	public void testAuthenticationWarning() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var sut = new IoShellyPlusPlugsImpl();
		
		// Pre-set the response for the /shelly endpoint with authentication enabled
		httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
			{
				"name": "shellyplusplugs-test",
				"id": "shellyplusplugs-12345",
				"mac": "AA:BB:CC:DD:EE:FF",
				"model": "SNSW-001P16EU",
				"gen": 2,
				"fw_id": "20230912-114516/v1.14.0-gcb84623",
				"ver": "1.14.0",
				"app": "PlusPlugS",
				"auth_en": true,
				"auth_domain": "shellyplusplugs-12345"
			}
		"""));
		
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setPhase(L1) //
						.setIp("127.0.0.1") //
						.setType(PRODUCTION) //
						.build()) //
				.next(new TestCase("Authentication enabled warning") //
						.output(ShellyCommon.ChannelId.AUTH_ENABLED_WARNING, true) //
						.output(ShellyCommon.ChannelId.DEVICE_GENERATION, "2")) //
				.deactivate();//
	}
}
