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
								   "wifi_sta": {
								     "connected": true,
								     "ssid": "",
								     "ip": "127.0.0.1",
								     "rssi": -30
								   },
								   "cloud": {
								     "enabled": false,
								     "connected": false
								   },
								   "mqtt": {
								     "connected": false
								   },
								   "time": "20:16",
								   "unixtime": 1747505765,
								   "serial": 6294,
								   "has_update": false,
								   "mac": "",
								   "cfg_changed_cnt": 0,
								   "actions_stats": {
								     "skipped": 0
								   },
								   "relays": [
								     {
								       "ison": false,
								       "has_timer": false,
								       "timer_started": 0,
								       "timer_duration": 0,
								       "timer_remaining": 0,
								       "overpower": false,
								       "is_valid": true,
								       "source": "input"
								     }
								   ],
								   "emeters": [
								     {
								       "power": 4,
								       "reactive": 0,
								       "pf": -0.07,
								       "voltage": 219.97,
								       "is_valid": true,
								       "total": 3301893.8,
								       "total_returned": 785.7
								     },
								     {
								       "power": 5,
								       "reactive": -12.38,
								       "pf": -0.38,
								       "voltage": 219.97,
								       "is_valid": true,
								       "total": 683493.3,
								       "total_returned": 194.1
								     }
								   ],
								   "update": {
								     "status": "idle",
								     "has_update": false,
								     "new_version": "20230913-114150/v1.14.0-gcb84623",
								     "old_version": "20230913-114150/v1.14.0-gcb84623",
								     "beta_version": "20231107-164916/v1.14.1-rc1-g0617c15"
								   },
								   "ram_total": 51064,
								   "ram_free": 33076,
								   "fs_size": 233681,
								   "fs_free": 150349,
								   "uptime": 281011
								 }
								"""));
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("-|9 W", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 9) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 9) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 219970) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 219970) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null) //
						.output(ElectricityMeter.ChannelId.CURRENT, 0) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 0) //
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
