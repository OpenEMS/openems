package io.openems.edge.evcs.hardybarth.ecb1;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
import static io.openems.edge.evcs.api.Phases.THREE_PHASE;
import static io.openems.edge.evcs.api.Status.CHARGING;
import static io.openems.edge.evcs.api.Status.NOT_READY_FOR_CHARGING;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.bridge.http.cycle.dummy.DummyCycleSubscriber;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;

class EvcsHardyBarthEcb1ImplTest {

	/** Chargecontrol response for a stopped (no car) scenario – from a real device. */
	private static final String CHARGECONTROL_NO_CAR = """
			{
			  "chargecontrol": {
			    "modeid": 3,
			    "evminamp": 6,
			    "vendor": "Phoenix Contact",
			    "name": "evcc1",
			    "mode": "manual",
			    "type": "EVCC Basic (RS485)",
			    "id": 1,
			    "supplylinemaxamp": 32,
			    "connected": false,
			    "manualmodeamp": 10.0,
			    "stateid": 17,
			    "version": "V1.3.1",
			    "busid": 1,
			    "currentpwmamp": 0.0,
			    "state": "A\\u2019"
			  },
			  "protocol-version": "1.4"
			}
			""";

	/** Chargecontrol response for a car connected but charging paused. */
	private static final String CHARGECONTROL_CAR_PAUSED = """
			{
			  "chargecontrol": {
			    "mode": "manual",
			    "connected": true,
			    "manualmodeamp": 0.0,
			    "stateid": 17,
			    "currentpwmamp": 0.0,
			    "state": "B",
			    "vendor": "Phoenix Contact",
			    "version": "V1.3.1"
			  },
			  "protocol-version": "1.4"
			}
			""";

	/** Chargecontrol response for active charging. */
	private static final String CHARGECONTROL_CHARGING = """
			{
			  "chargecontrol": {
			    "mode": "manual",
			    "connected": true,
			    "manualmodeamp": 16.0,
			    "stateid": 5,
			    "currentpwmamp": 16.0,
			    "state": "C",
			    "vendor": "Phoenix Contact",
			    "version": "V1.3.1"
			  },
			  "protocol-version": "1.4"
			}
			""";

	/** Meter response from a real device (standby values). */
	private static final String METER_RESPONSE = """
			{
			  "meter": {
			    "serial": 75740051,
			    "vendor": "eCHARGE",
			    "type": "eCB1 intern",
			    "name": "Carport",
			    "data": {
			      "1-0:1.4.0": 3.5,
			      "1-0:1.8.0": 6541.3684,
			      "1-0:21.4.0": 3.5,
			      "1-0:41.4.0": 0.0,
			      "1-0:61.4.0": 0.0,
			      "1-0:31.4.0": 0.018,
			      "1-0:51.4.0": 0.0,
			      "1-0:71.4.0": 0.0,
			      "1-0:32.4.0": 228.373,
			      "1-0:52.4.0": 229.971,
			      "1-0:72.4.0": 228.620
			    },
			    "id": 1,
			    "ipaddress": "127.0.0.1",
			    "function": "socket"
			  },
			  "protocol-version": "1.4"
			}
			""";

	/** Meter response for a charging scenario with clean numbers. */
	private static final String METER_CHARGING = """
			{
			  "meter": {
			    "serial": 75740051,
			    "vendor": "eCHARGE",
			    "type": "eCB1 intern",
			    "name": "Carport",
			    "data": {
			      "1-0:1.4.0": 11040.0,
			      "1-0:1.8.0": 10000.0,
			      "1-0:21.4.0": 3680.0,
			      "1-0:41.4.0": 3680.0,
			      "1-0:61.4.0": 3680.0,
			      "1-0:31.4.0": 16.0,
			      "1-0:51.4.0": 16.0,
			      "1-0:71.4.0": 16.0,
			      "1-0:32.4.0": 230.0,
			      "1-0:52.4.0": 230.0,
			      "1-0:72.4.0": 230.0
			    },
			    "id": 1,
			    "ipaddress": "127.0.0.1",
			    "function": "socket"
			  },
			  "protocol-version": "1.4"
			}
			""";

	@Test
	void testNoCar() throws Exception {
		var sut = new EvcsHardyBarthEcb1Impl();
		var test = new ComponentTest(sut) //
				.addReference("httpBridgeFactory",
						ofBridgeImpl(DummyBridgeHttpFactory::dummyEndpointFetcher,
								DummyBridgeHttpFactory::dummyBridgeHttpExecutor)) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber())) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setIp("192.168.2.8") //
						.setChargeControlId(1) //
						.setMeterId(1) //
						.setMinHwCurrent(6_000) //
						.setMaxHwCurrent(32_000) //
						.build());
		var handler = ReflectionUtils.<Ecb1Handler>getValueViaReflection(sut, "handler");
		test //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							handler.handleChargeControlResponse(CHARGECONTROL_NO_CAR);
							handler.handleMeterResponse(METER_RESPONSE);
						}) //
						.output(Evcs.ChannelId.STATUS, NOT_READY_FOR_CHARGING) //
						.output(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED, false) //
						.output(Evcs.ChannelId.CHARGING_TYPE, ChargingType.AC) //
						.output(Evcs.ChannelId.PHASES, THREE_PHASE) //
						.output(Evcs.ChannelId.FIXED_MINIMUM_HARDWARE_POWER, 4140) //
						.output(Evcs.ChannelId.FIXED_MAXIMUM_HARDWARE_POWER, 22080) //

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 4) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 4) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(ElectricityMeter.ChannelId.CURRENT, 18) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 18) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 0) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 0) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 228_988) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 228_373) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 229_971) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 228_620) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 6541L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 6541L) //

						.output(EvcsHardyBarthEcb1.ChannelId.RAW_STATE_ID, 17) //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_STATE, "A’") //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_MODE, "manual") //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_CONNECTED, false) //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_MANUAL_MODE_AMP, 10.0) //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_CURRENT_PWM_AMP, 0.0) //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_VENDOR, "Phoenix Contact") //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_VERSION, "V1.3.1") //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_METER_SERIAL, 75740051) //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_METER_VENDOR, "eCHARGE") //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_METER_TYPE, "eCB1 intern") //
				);
	}

	@Test
	void testCharging() throws Exception {
		var sut = new EvcsHardyBarthEcb1Impl();
		var test = new ComponentTest(sut) //
				.addReference("httpBridgeFactory",
						ofBridgeImpl(DummyBridgeHttpFactory::dummyEndpointFetcher,
								DummyBridgeHttpFactory::dummyBridgeHttpExecutor)) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber())) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setIp("192.168.2.8") //
						.setChargeControlId(1) //
						.setMeterId(1) //
						.setMinHwCurrent(6_000) //
						.setMaxHwCurrent(32_000) //
						.build());
		var handler = ReflectionUtils.<Ecb1Handler>getValueViaReflection(sut, "handler");
		test //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							handler.handleChargeControlResponse(CHARGECONTROL_CHARGING);
							handler.handleMeterResponse(METER_CHARGING);
						}) //
						.output(Evcs.ChannelId.STATUS, CHARGING) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 11040) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 3680) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 3680) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 3680) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 16_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 16_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 16_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 230_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 230_000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 10000L) //

						.output(EvcsHardyBarthEcb1.ChannelId.RAW_STATE_ID, 5) //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_STATE, "C") //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_CONNECTED, true) //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_MANUAL_MODE_AMP, 16.0) //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_CURRENT_PWM_AMP, 16.0) //
				);
	}

	@Test
	void testCarConnectedPaused() throws Exception {
		var sut = new EvcsHardyBarthEcb1Impl();
		var test = new ComponentTest(sut) //
				.addReference("httpBridgeFactory",
						ofBridgeImpl(DummyBridgeHttpFactory::dummyEndpointFetcher,
								DummyBridgeHttpFactory::dummyBridgeHttpExecutor)) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber())) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setIp("192.168.2.8") //
						.setChargeControlId(1) //
						.setMeterId(1) //
						.setMinHwCurrent(6_000) //
						.setMaxHwCurrent(32_000) //
						.build());
		var handler = ReflectionUtils.<Ecb1Handler>getValueViaReflection(sut, "handler");
		test //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> handler.handleChargeControlResponse(CHARGECONTROL_CAR_PAUSED)) //
						.output(Evcs.ChannelId.STATUS, Status.CHARGING_REJECTED) //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_STATE, "B") //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_CONNECTED, true) //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_STATE_ID, 17) //
				);
	}

	@Test
	void testSetCurrentSendsHttpRequests() throws Exception {
		final var pool = DummyBridgeHttpFactory.dummyBridgeHttpExecutor(false);
		final var httpBundle = DummyBridgeHttpBundle.of(pool);
		final var sentBodies = new java.util.ArrayList<String>();
		final var sentUrls = new java.util.ArrayList<String>();

		httpBundle.fetcher().addEndpointHandler(ep -> {
			sentUrls.add(ep.url());
			if (ep.body() != null) {
				sentBodies.add(ep.body());
			}
			return HttpResponse.ok("ok");
		});

		var sut = new EvcsHardyBarthEcb1Impl();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpBundle.factory()) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber())) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setIp("192.168.2.8") //
						.setChargeControlId(1) //
						.setMeterId(1) //
						.setMinHwCurrent(6_000) //
						.setMaxHwCurrent(32_000) //
						.build());

		// Trigger applyChargePowerLimit at 6900 W (3 phases × 10 A × 230 V)
		sut.applyChargePowerLimit(6900);

		// Flush the executor so requests are actually dispatched
		pool.update();

		// Expect set-current and start requests
		assertTrue(sentUrls.stream().anyMatch(u -> u.contains("/mode/manual/ampere")),
				"Expected a manualmodeamp POST");
		assertTrue(sentUrls.stream().anyMatch(u -> u.contains("/start")),
				"Expected a start POST");
		assertTrue(sentBodies.stream().anyMatch(b -> b.contains("manualmodeamp=10")),
				"Expected manualmodeamp=10 (6900 W / 3 / 230 = 10 A)");
	}

	@Test
	void testPauseStopsCharging() throws Exception {
		final var pool = DummyBridgeHttpFactory.dummyBridgeHttpExecutor(false);
		final var httpBundle = DummyBridgeHttpBundle.of(pool);
		final var sentUrls = new java.util.ArrayList<String>();

		httpBundle.fetcher().addEndpointHandler(ep -> {
			sentUrls.add(ep.url());
			return HttpResponse.ok("ok");
		});

		var sut = new EvcsHardyBarthEcb1Impl();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpBundle.factory()) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber())) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setIp("192.168.2.8") //
						.setChargeControlId(1) //
						.setMeterId(1) //
						.setMinHwCurrent(6_000) //
						.setMaxHwCurrent(32_000) //
						.build());

		sut.pauseChargeProcess();
		pool.update();

		assertTrue(sentUrls.stream().anyMatch(u -> u.contains("/stop")),
				"Expected a stop POST");
	}
}
