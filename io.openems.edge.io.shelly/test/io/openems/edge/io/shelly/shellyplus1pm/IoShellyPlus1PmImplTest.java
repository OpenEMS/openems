package io.openems.edge.io.shelly.shellyplus1pm;

import static io.openems.edge.meter.api.MeterType.CONSUMPTION_METERED;
import static io.openems.edge.meter.api.SinglePhase.L1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.timedata.test.DummyTimedata;

public class IoShellyPlus1PmImplTest {

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
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var sut = new IoShellyPlus1PmImpl();

		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("127.0.0.1") //
						.setType(CONSUMPTION_METERED) //
						.setPhase(L1) //
						.build()) //

				.next(new TestCase("Successful read response") //
						.onBeforeControllersCallbacks(() -> {
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
							httpTestBundle.triggerNextCycle();
						}) //
						.output(ACTIVE_POWER, 123) //
						.output(ACTIVE_POWER_L1, 123) //
						.output(ACTIVE_POWER_L2, null) //
						.output(CURRENT, 500) //
						.output(VOLTAGE, 231300)) //

				.next(new TestCase("Invalid read response") //
						.onBeforeControllersCallbacks(() -> assertEquals("Off|123 W", sut.debugLog()))

						.onBeforeControllersCallbacks(() -> {
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							httpTestBundle.triggerNextCycle();
						}) //
						.output(ACTIVE_POWER, null) //
						.output(ACTIVE_POWER_L1, null) //
						.output(ACTIVE_POWER_L2, null) //
						.output(CURRENT, null) //
						.output(VOLTAGE, null) //

						.output(PRODUCTION_ENERGY, 0L) //
						.output(CONSUMPTION_ENERGY, 0L)) //

				.next(new TestCase("Write") //
						.onBeforeControllersCallbacks(() -> assertEquals("Unknown|UNDEFINED", sut.debugLog()))
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

				.deactivate();
	}

}
