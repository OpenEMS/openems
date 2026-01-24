package io.openems.edge.io.shelly.shellyplus1pm;

import static io.openems.common.types.MeterType.CONSUMPTION_METERED;
import static io.openems.edge.common.type.Phase.SinglePhase.L1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.bridge.http.cycle.dummy.DummyCycleSubscriber;
import org.junit.Test;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.test.DummyTimedata;

public class IoShellyPlus1PmImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new IoShellyPlus1PmImpl();
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var dummyCycleSubscriber = new DummyCycleSubscriber();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(dummyCycleSubscriber)) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIp("127.0.0.1") //
						.setType(CONSUMPTION_METERED) //
						.setPhase(L1) //
						.setInvert(false) //
						.build()) //

				.next(new TestCase("Successful read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									   "ble":{
									   },
									   "cloud":{
									      "connected":true
									   },
									   "input:0":{
									      "id":0,
									      "state":false
									   },
									   "mqtt":{
									      "connected":false
									   },
									   "switch:0":{
									      "id":0,
									      "source":"init",
									      "output":false,
									      "apower":123.0,
									      "voltage":231.3,
									      "current":0.500,
									      "aenergy":{
									         "total":8629.000,
									         "by_minute":[
									            0.000,
									            0.000,
									            0.000
									         ],
									         "minute_ts":1708858380
									      },
									      "temperature":{
									         "tC":44.3,
									         "tF":111.7
									      }
									   },
									   "sys":{
									      "mac":"80646FE34998",
									      "restart_required":false,
									      "time":"11:53",
									      "unixtime":1708858386,
									      "uptime":150390,
									      "ram_size":260364,
									      "ram_free":115308,
									      "fs_size":458752,
									      "fs_free":143360,
									      "cfg_rev":22,
									      "kvs_rev":2,
									      "schedule_rev":0,
									      "webhook_rev":0,
									      "available_updates":{
									      },
									      "reset_reason":3
									   },
									   "wifi":{
									      "sta_ip":"192.168.178.169",
									      "status":"got ip",
									      "ssid":"heizung",
									      "rssi":-48,
									      "ap_client_count":0
									   },
									   "ws":{
									      "connected":false
									   }
									}
									"""));
							dummyCycleSubscriber.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("-|123 W", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 123) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 123) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 231300) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 231300) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null) //
						.output(ElectricityMeter.ChannelId.CURRENT, 500) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 500) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
						.output(IoShellyPlus1Pm.ChannelId.RELAY, null) //
						.output(IoShellyPlus1Pm.ChannelId.SLAVE_COMMUNICATION_FAILED, false)) //

				.next(new TestCase("Invalid read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							dummyCycleSubscriber.triggerNextCycle();
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
						.output(IoShellyPlus1Pm.ChannelId.RELAY, null) //
						.output(IoShellyPlus1Pm.ChannelId.SLAVE_COMMUNICATION_FAILED, true)) //

				.next(new TestCase("Write") //
						.onBeforeControllersCallbacks(() -> sut.setRelay(true)) //
						.also(testCase -> {
							final var relayTurnedOn = httpTestBundle.expect("http://127.0.0.1/relay/0?turn=on")
									.toBeCalled();

							testCase.onBeforeControllersCallbacks(dummyCycleSubscriber::triggerNextCycle);
							testCase.onAfterWriteCallbacks(
									() -> assertTrue("Failed to turn on relay", relayTurnedOn.get()));
						})) //

				.deactivate();
	}

	@Test
	public void testInvert() throws Exception {
		final var sut = new IoShellyPlus1PmImpl();
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var dummyCycleSubscriber = new DummyCycleSubscriber();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(dummyCycleSubscriber)) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIp("127.0.0.1") //
						.setType(CONSUMPTION_METERED) //
						.setPhase(L1) //
						.setInvert(true) //
						.build()) //

				.next(new TestCase("Successful read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									   "ble":{
									   },
									   "cloud":{
									      "connected":true
									   },
									   "input:0":{
									      "id":0,
									      "state":false
									   },
									   "mqtt":{
									      "connected":false
									   },
									   "switch:0":{
									      "id":0,
									      "source":"init",
									      "output":false,
									      "apower":123.0,
									      "voltage":231.3,
									      "current":0.500,
									      "aenergy":{
									         "total":8629.000,
									         "by_minute":[
									            0.000,
									            0.000,
									            0.000
									         ],
									         "minute_ts":1708858380
									      },
									      "temperature":{
									         "tC":44.3,
									         "tF":111.7
									      }
									   },
									   "sys":{
									      "mac":"80646FE34998",
									      "restart_required":false,
									      "time":"11:53",
									      "unixtime":1708858386,
									      "uptime":150390,
									      "ram_size":260364,
									      "ram_free":115308,
									      "fs_size":458752,
									      "fs_free":143360,
									      "cfg_rev":22,
									      "kvs_rev":2,
									      "schedule_rev":0,
									      "webhook_rev":0,
									      "available_updates":{
									      },
									      "reset_reason":3
									   },
									   "wifi":{
									      "sta_ip":"192.168.178.169",
									      "status":"got ip",
									      "ssid":"heizung",
									      "rssi":-48,
									      "ap_client_count":0
									   },
									   "ws":{
									      "connected":false
									   }
									}
									"""));
							dummyCycleSubscriber.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("-|-123 W", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, -123) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, -123) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 231300) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 231300) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null) //
						.output(ElectricityMeter.ChannelId.CURRENT, -500) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, -500) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
						.output(IoShellyPlus1Pm.ChannelId.RELAY, null) //
						.output(IoShellyPlus1Pm.ChannelId.SLAVE_COMMUNICATION_FAILED, false));
	}
}