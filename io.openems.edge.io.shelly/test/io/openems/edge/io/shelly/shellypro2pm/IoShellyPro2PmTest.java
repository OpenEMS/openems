package io.openems.edge.io.shelly.shellypro2pm;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.bridge.http.cycle.dummy.DummyCycleSubscriber;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.type.Phase;
import io.openems.edge.io.shelly.common.HttpBridgeShellyService;
import io.openems.edge.io.shelly.common.component.ShellyMeteredSwitch;
import io.openems.edge.io.shelly.common.component.ShellySwitch;
import io.openems.edge.io.shelly.common.gen2.IoGen2ShellyBase;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.test.DummyTimedata;

public class IoShellyPro2PmTest {
	@Test
	public void test() throws Exception {
		final var device = new IoShellyPro2PmDeviceImpl();
		final var terminal1 = new IoShellyPro2PmTerminalImpl();
		final var terminal2 = new IoShellyPro2PmTerminalImpl();
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var dummyCycleSubscriber = new DummyCycleSubscriber();

		final var deviceTest = new ComponentTest(device) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(dummyCycleSubscriber)) //
				.addReference("httpBridgeShellyServiceDefinition",
						new HttpBridgeShellyService.HttpBridgeShellyServiceDefinition()) //
				.activate(MyDeviceConfig.create() //
						.setId("io0") //
						.setIp("127.0.0.1") //
						.setPhase(Phase.SinglePhase.L2) //
						.build());

		final var terminal1Test = new ComponentTest(terminal1).addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setDevice", device) //
				.activate(MyTerminalConfig.create() //
						.setId("io1") //
						.setDeviceId("io0") //
						.setTerminal(TerminalEnum.RELAY_1) //
						.setInvert(false) //
						.setType(MeterType.CONSUMPTION_METERED) //
						.build());
		;

		final var terminal2Test = new ComponentTest(terminal2).addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setDevice", device) //
				.activate(MyTerminalConfig.create() //
						.setId("io2") //
						.setDeviceId("io0") //
						.setTerminal(TerminalEnum.RELAY_2) //
						.setInvert(true) //
						.setType(MeterType.GRID) //
						.build()) //

				.next(new AbstractComponentTest.TestCase("Successful read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									  "ble": {},
									  "cloud": {
									    "connected": true
									  },
									  "eth": {
									    "ip": null
									  },
									  "input:0": {
									    "id": 0,
									    "state": false
									  },
									  "input:1": {
									    "id": 1,
									    "state": false
									  },
									  "mqtt": {
									    "connected": false
									  },
									  "switch:0": {
									    "id": 0,
									    "source": "init",
									    "output": false,
									    "apower": 45.0,
									    "voltage": 231.0,
									    "freq": 51.0,
									    "current": 2.0,
									    "pf": 1.00,
									    "aenergy": {
									      "total": 0.000,
									      "by_minute": [
									        0.000,
									        0.000,
									        0.000
									      ],
									      "minute_ts": 1771846740
									    },
									    "ret_aenergy": {
									      "total": 0.000,
									      "by_minute": [
									        0.000,
									        0.000,
									        0.000
									      ],
									      "minute_ts": 1771846740
									    },
									    "temperature": {
									      "tC": 34.8,
									      "tF": 94.6
									    },
									    "errors": [
									      "overtemp"
									    ]
									  },
									  "switch:1": {
									    "id": 1,
									    "source": "init",
									    "output": false,
									    "apower": -44.0,
									    "voltage": 230.0,
									    "freq": 50.0,
									    "current": -1.0,
									    "pf": 1.00,
									    "aenergy": {
									      "total": 0.000,
									      "by_minute": [
									        0.000,
									        0.000,
									        0.000
									      ],
									      "minute_ts": 1771846740
									    },
									    "ret_aenergy": {
									      "total": 0.000,
									      "by_minute": [
									        0.000,
									        0.000,
									        0.000
									      ],
									      "minute_ts": 1771846740
									    },
									    "temperature": {
									      "tC": 38.2,
									      "tF": 100.8
									    }
									  },
									  "sys": {
									    "mac": "EC6240A061BB",
									    "restart_required": false,
									    "time": "12:39",
									    "unixtime": 1771846756,
									    "uptime": 2874,
									    "ram_size": 243316,
									    "ram_free": 118740,
									    "fs_size": 524288,
									    "fs_free": 196608,
									    "cfg_rev": 11,
									    "kvs_rev": 0,
									    "schedule_rev": 0,
									    "webhook_rev": 0,
									    "available_updates": {
									      "stable": {
									        "version": "1.7.4"
									      }
									    },
									    "reset_reason": 1
									  },
									  "wifi": {
									    "sta_ip": "192.168.178.46",
									    "status": "got ip",
									    "ssid": "TEST",
									    "rssi": -43
									  },
									  "ws": {
									    "connected": false
									  }
									}
									"""));
							dummyCycleSubscriber.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> {
						}) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 44) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 44) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 230000) //
						.output(ElectricityMeter.ChannelId.CURRENT, 1000) //
						.output(ShellySwitch.ChannelId.RELAY, null));

		deviceTest.next(new AbstractComponentTest.TestCase("Successful response") //
				.output(IoGen2ShellyBase.ChannelId.HAS_UPDATE, true) //
				.output(IoGen2ShellyBase.ChannelId.SLAVE_COMMUNICATION_FAILED, false) //
				.output(IoGen2ShellyBase.ChannelId.WRONG_DEVICE_TYPE, false)); //

		terminal1Test.next(new AbstractComponentTest.TestCase("Successful response") //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 45) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 45) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
				.output(ElectricityMeter.ChannelId.VOLTAGE, 231000) //
				.output(ElectricityMeter.ChannelId.CURRENT, 2000) //
				.output(ShellyMeteredSwitch.ErrorChannelId.OVERTEMP, true) //
				.output(ShellyMeteredSwitch.ErrorChannelId.OVERPOWER, false) //
				.output(ShellySwitch.ChannelId.RELAY, null));

		terminal2Test.next(new AbstractComponentTest.TestCase("Write") //
				.onBeforeControllersCallbacks(() -> {
					terminal2.setRelay(true);
				}) //
				.also(testCase -> {
					final var relayTurnedOn = httpTestBundle.expect("http://127.0.0.1/rpc/Switch.Set?id=1&on=true")
							.toBeCalled();

					testCase.onBeforeControllersCallbacks(dummyCycleSubscriber::triggerNextCycle);
					testCase.onAfterWriteCallbacks(() -> assertTrue("Failed to turn on relay", relayTurnedOn.get()));
				}));

		this.testNoCommunication(deviceTest, terminal1Test, terminal2Test, httpTestBundle, dummyCycleSubscriber);

		terminal1Test.deactivate();
		terminal2Test.deactivate();
		deviceTest.deactivate();
	}

	private void testNoCommunication(ComponentTest deviceTest, ComponentTest terminal1Test, ComponentTest terminal2Test,
			DummyBridgeHttpBundle httpTestBundle, DummyCycleSubscriber dummyCycleSubscriber) throws Exception {
		deviceTest.next(new AbstractComponentTest.TestCase("No communication") //
				.onBeforeProcessImage(() -> {
					httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
					dummyCycleSubscriber.triggerNextCycle();
				}) //
				.output(IoGen2ShellyBase.ChannelId.SLAVE_COMMUNICATION_FAILED, true)); //

		terminal1Test.next(new AbstractComponentTest.TestCase("No communication - T1") //
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
				.output(ElectricityMeter.ChannelId.CURRENT_L3, null));

		terminal2Test.next(new AbstractComponentTest.TestCase("No communication - T2") //
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
				.output(ElectricityMeter.ChannelId.CURRENT_L3, null));
	}
}
