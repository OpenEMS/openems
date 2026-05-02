package io.openems.edge.io.shelly.shellypro4pm;

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

public class IoShellyPro4PmTest {
	@Test
	public void test() throws Exception {
		final var device = new IoShellyPro4PmDeviceImpl();
		final var terminal1 = new IoShellyPro4PmTerminalImpl();
		final var terminal2 = new IoShellyPro4PmTerminalImpl();
		final var terminal3 = new IoShellyPro4PmTerminalImpl();
		final var terminal4 = new IoShellyPro4PmTerminalImpl();
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
						.setPhase(Phase.SinglePhase.L1) //
						.build());

		var terminal1Test = new ComponentTest(terminal1).addReference("timedata", new DummyTimedata("timedata0")) //
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
						.build());

		final var terminal3Test = new ComponentTest(terminal3).addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setDevice", device) //
				.activate(MyTerminalConfig.create() //
						.setId("io3") //
						.setDeviceId("io0") //
						.setTerminal(TerminalEnum.RELAY_3) //
						.setInvert(false) //
						.setType(MeterType.CONSUMPTION_METERED) //
						.build());

		final var terminal4Test = new ComponentTest(terminal4).addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setDevice", device) //
				.activate(MyTerminalConfig.create() //
						.setId("io4") //
						.setDeviceId("io0") //
						.setTerminal(TerminalEnum.RELAY_4) //
						.setInvert(false) //
						.setType(MeterType.CONSUMPTION_METERED) //
						.build());

		terminal4Test.next(new AbstractComponentTest.TestCase("Successful read response") //
				.onBeforeProcessImage(() -> {
					httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
							{
							  "ble": {},
							  "bthome": {
							    "errors": [
							      "bluetooth_disabled"
							    ]
							  },
							  "cloud": {
							    "connected": true
							  },
							  "eth": {
							    "ip": null,
							    "ip6": null
							  },
							  "input:0": {
							    "id": 0,
							    "state": false
							  },
							  "input:1": {
							    "id": 1,
							    "state": false
							  },
							  "input:2": {
							    "id": 2,
							    "state": false
							  },
							  "input:3": {
							    "id": 3,
							    "state": false
							  },
							  "knx": {},
							  "mqtt": {
							    "connected": false
							  },
							  "switch:0": {
							    "id": 0,
							    "source": "init",
							    "output": false,
							    "apower": 40.0,
							    "voltage": 229.3,
							    "freq": 50.0,
							    "current": 2.000,
							    "pf": 0.00,
							    "aenergy": {
							      "total": 0.000,
							      "by_minute": [
							        0.000,
							        0.000,
							        0.000
							      ],
							      "minute_ts": 1771925700
							    },
							    "ret_aenergy": {
							      "total": 0.000,
							      "by_minute": [
							        0.000,
							        0.000,
							        0.000
							      ],
							      "minute_ts": 1771925700
							    },
							    "temperature": {
							      "tC": 36.4,
							      "tF": 97.5
							    },
							    "errors": [
							      "overtemp"
							    ]
							  },
							  "switch:1": {
							    "id": 1,
							    "source": "init",
							    "output": false,
							    "apower": -120.0,
							    "voltage": 229.5,
							    "freq": 50.0,
							    "current": -4.000,
							    "pf": 0.00,
							    "aenergy": {
							      "total": 0.000,
							      "by_minute": [
							        0.000,
							        0.000,
							        0.000
							      ],
							      "minute_ts": 1771925700
							    },
							    "ret_aenergy": {
							      "total": 0.000,
							      "by_minute": [
							        0.000,
							        0.000,
							        0.000
							      ],
							      "minute_ts": 1771925700
							    },
							    "temperature": {
							      "tC": 36.4,
							      "tF": 97.5
							    }
							  },
							  "switch:2": {
							    "id": 2,
							    "source": "init",
							    "output": false,
							    "apower": 0.0,
							    "voltage": 229.2,
							    "freq": 50.0,
							    "current": 0.000,
							    "pf": 0.00,
							    "aenergy": {
							      "total": 0.000,
							      "by_minute": [
							        0.000,
							        0.000,
							        0.000
							      ],
							      "minute_ts": 1771925700
							    },
							    "ret_aenergy": {
							      "total": 0.000,
							      "by_minute": [
							        0.000,
							        0.000,
							        0.000
							      ],
							      "minute_ts": 1771925700
							    },
							    "temperature": {
							      "tC": 36.4,
							      "tF": 97.5
							    }
							  },
							  "switch:3": {
							    "id": 3,
							    "source": "init",
							    "output": false,
							    "apower": 22.5,
							    "voltage": 229.1,
							    "freq": 50.0,
							    "current": 1.000,
							    "pf": 0.00,
							    "aenergy": {
							      "total": 0.000,
							      "by_minute": [
							        0.000,
							        0.000,
							        0.000
							      ],
							      "minute_ts": 1771925700
							    },
							    "ret_aenergy": {
							      "total": 0.000,
							      "by_minute": [
							        0.000,
							        0.000,
							        0.000
							      ],
							      "minute_ts": 1771925700
							    },
							    "temperature": {
							      "tC": 36.4,
							      "tF": 97.5
							    }
							  },
							  "sys": {
							    "mac": "5C013B04EB34",
							    "restart_required": false,
							    "time": "10:35",
							    "unixtime": 1771925712,
							    "last_sync_ts": 1771925555,
							    "uptime": 268,
							    "ram_size": 248244,
							    "ram_free": 89424,
							    "ram_min_free": 84624,
							    "fs_size": 524288,
							    "fs_free": 192512,
							    "cfg_rev": 11,
							    "kvs_rev": 0,
							    "schedule_rev": 0,
							    "webhook_rev": 0,
							    "btrelay_rev": 0,
							    "available_updates": {
							      "stable": {
							        "version": "1.7.4"
							      }
							    },
							    "reset_reason": 1,
							    "utc_offset": 3600
							  },
							  "ui": {},
							  "wifi": {
							    "sta_ip": "192.168.178.40",
							    "status": "got ip",
							    "ssid": "TEST",
							    "rssi": -50,
							    "sta_ip6": [
							      "fe80::5e01:3bff:fe04:eb34",
							      "fde9:ba7f:131c:0:5e01:3bff:fe04:eb34"
							    ]
							  },
							  "ws": {
							    "connected": false
							  }
							}
							"""));
					dummyCycleSubscriber.triggerNextCycle();
				}) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 23) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 23) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
				.output(ElectricityMeter.ChannelId.VOLTAGE, 229100) //
				.output(ElectricityMeter.ChannelId.CURRENT, 1000) //
				.output(ShellySwitch.ChannelId.RELAY, null));

		deviceTest.next(new AbstractComponentTest.TestCase("Successful response") //
				.output(IoGen2ShellyBase.ChannelId.HAS_UPDATE, true) //
				.output(IoGen2ShellyBase.ChannelId.SLAVE_COMMUNICATION_FAILED, false) //
				.output(IoGen2ShellyBase.ChannelId.WRONG_DEVICE_TYPE, false)); //

		terminal1Test.next(new AbstractComponentTest.TestCase("Successful response") //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 40) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 40) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
				.output(ElectricityMeter.ChannelId.VOLTAGE, 229300) //
				.output(ElectricityMeter.ChannelId.CURRENT, 2000) //
				.output(ShellyMeteredSwitch.ErrorChannelId.OVERTEMP, true) //
				.output(ShellyMeteredSwitch.ErrorChannelId.OVERPOWER, false) //
				.output(ShellySwitch.ChannelId.RELAY, null));

		terminal2Test.next(new AbstractComponentTest.TestCase("Successful response") //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 120) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 120) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
				.output(ElectricityMeter.ChannelId.VOLTAGE, 229500) //
				.output(ElectricityMeter.ChannelId.CURRENT, 4000) //
				.output(ShellySwitch.ChannelId.RELAY, null));

		terminal3Test.next(new AbstractComponentTest.TestCase("Write") //
				.onBeforeControllersCallbacks(() -> {
					terminal3.setRelay(true);
				}) //
				.also(testCase -> {
					final var relayTurnedOn = httpTestBundle.expect("http://127.0.0.1/rpc/Switch.Set?id=2&on=true")
							.toBeCalled();

					testCase.onBeforeControllersCallbacks(dummyCycleSubscriber::triggerNextCycle);
					testCase.onAfterWriteCallbacks(() -> assertTrue("Failed to turn on relay", relayTurnedOn.get()));
				}));

		this.testNoCommunication(deviceTest, terminal1Test, terminal2Test, terminal3Test, terminal4Test, httpTestBundle,
				dummyCycleSubscriber);

		terminal1Test.deactivate();
		terminal2Test.deactivate();
		terminal3Test.deactivate();
		terminal4Test.deactivate();
		deviceTest.deactivate();
	}

	private void testNoCommunication(ComponentTest deviceTest, ComponentTest terminal1Test, ComponentTest terminal2Test,
			ComponentTest terminal3Test, ComponentTest terminal4Test, DummyBridgeHttpBundle httpTestBundle,
			DummyCycleSubscriber dummyCycleSubscriber) throws Exception {
		deviceTest.next(new AbstractComponentTest.TestCase("No communication") //
				.onBeforeProcessImage(() -> {
					httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
					dummyCycleSubscriber.triggerNextCycle();
				}) //
				.output(IoGen2ShellyBase.ChannelId.SLAVE_COMMUNICATION_FAILED, true));

		this.testNoCommunicationOnTerminal(terminal1Test);
		this.testNoCommunicationOnTerminal(terminal2Test);
		this.testNoCommunicationOnTerminal(terminal3Test);
		this.testNoCommunicationOnTerminal(terminal4Test);
	}

	private void testNoCommunicationOnTerminal(ComponentTest terminalTest) throws Exception {
		terminalTest.next(new AbstractComponentTest.TestCase("No communication") //
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
