package io.openems.edge.evse.chargepoint.hardybarth.ecb1;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
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
import io.openems.edge.evcs.hardybarth.ecb1.Ecb1Handler;
import io.openems.edge.evcs.hardybarth.ecb1.EvcsHardyBarthEcb1;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.meter.api.ElectricityMeter;

class EvseChargePointHardyBarthEcb1ImplTest {

	/** Chargecontrol response for a stopped (no car) scenario. */
	private static final String CHARGECONTROL_NO_CAR = """
			{
			  "chargecontrol": {
			    "mode": "manual",
			    "connected": false,
			    "manualmodeamp": 10.0,
			    "stateid": 17,
			    "currentpwmamp": 0.0,
			    "state": "A",
			    "vendor": "Phoenix Contact",
			    "version": "V1.3.1"
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

	/** Meter response with charging values. */
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
			    "id": 1
			  },
			  "protocol-version": "1.4"
			}
			""";

	private static ComponentTest buildTest(EvseChargePointHardyBarthEcb1Impl sut) throws Exception {
		return new ComponentTest(sut) //
				.addReference("httpBridgeFactory",
						ofBridgeImpl(DummyBridgeHttpFactory::dummyEndpointFetcher,
								DummyBridgeHttpFactory::dummyBridgeHttpExecutor)) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber())) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setIp("192.168.2.8") //
						.setChargeControlId(1) //
						.setMeterId(1) //
						.setMinHwCurrent(6_000) //
						.setMaxHwCurrent(32_000) //
						.build());
	}

	@Test
	void testNoCar() throws Exception {
		var sut = new EvseChargePointHardyBarthEcb1Impl();
		var test = buildTest(sut);
		var handler = ReflectionUtils.<Ecb1Handler>getValueViaReflection(sut, "handler");
		test //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> handler.handleChargeControlResponse(CHARGECONTROL_NO_CAR)) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false) //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_STATE, "A") //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_CONNECTED, false) //
				);
	}

	@Test
	void testCarPaused() throws Exception {
		var sut = new EvseChargePointHardyBarthEcb1Impl();
		var test = buildTest(sut);
		var handler = ReflectionUtils.<Ecb1Handler>getValueViaReflection(sut, "handler");
		test //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> handler.handleChargeControlResponse(CHARGECONTROL_CAR_PAUSED)) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true) //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_STATE, "B") //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_CONNECTED, true) //
				);
	}

	@Test
	void testCharging() throws Exception {
		var sut = new EvseChargePointHardyBarthEcb1Impl();
		var test = buildTest(sut);
		var handler = ReflectionUtils.<Ecb1Handler>getValueViaReflection(sut, "handler");
		test //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							handler.handleChargeControlResponse(CHARGECONTROL_CHARGING);
							handler.handleMeterResponse(METER_CHARGING);
						}) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true) //
						.output(EvcsHardyBarthEcb1.ChannelId.RAW_STATE, "C") //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 11040) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 16_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230_000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 10000L) //
				);
	}

	@Test
	void testSetCurrentSendsHttpRequests() throws Exception {
		final var pool = DummyBridgeHttpFactory.dummyBridgeHttpExecutor(false);
		final var httpBundle = DummyBridgeHttpBundle.of(pool);
		final var sentUrls = new java.util.ArrayList<String>();
		final var sentBodies = new java.util.ArrayList<String>();

		httpBundle.fetcher().addEndpointHandler(ep -> {
			sentUrls.add(ep.url());
			if (ep.body() != null) {
				sentBodies.add(ep.body());
			}
			return HttpResponse.ok("ok");
		});

		var sut = new EvseChargePointHardyBarthEcb1Impl();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpBundle.factory()) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber())) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setIp("192.168.2.8") //
						.setChargeControlId(1) //
						.setMeterId(1) //
						.setMinHwCurrent(6_000) //
						.setMaxHwCurrent(32_000) //
						.build());

		// Simulate the controller calling apply() with 10 A
		var handler = ReflectionUtils.<Ecb1Handler>getValueViaReflection(sut, "handler");
		handler.setTarget(10);
		pool.update();

		assertTrue(sentUrls.stream().anyMatch(u -> u.contains("/mode/manual/ampere")),
				"Expected a manualmodeamp POST");
		assertTrue(sentUrls.stream().anyMatch(u -> u.contains("/start")),
				"Expected a start POST");
		assertTrue(sentBodies.stream().anyMatch(b -> b.contains("manualmodeamp=10")),
				"Expected manualmodeamp=10");
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

		var sut = new EvseChargePointHardyBarthEcb1Impl();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpBundle.factory()) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber())) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setIp("192.168.2.8") //
						.setChargeControlId(1) //
						.setMeterId(1) //
						.setMinHwCurrent(6_000) //
						.setMaxHwCurrent(32_000) //
						.build());

		var handler = ReflectionUtils.<Ecb1Handler>getValueViaReflection(sut, "handler");
		handler.setTarget(0);
		pool.update();

		assertTrue(sentUrls.stream().anyMatch(u -> u.contains("/stop")),
				"Expected a stop POST");
		assertFalse(sentUrls.stream().anyMatch(u -> u.contains("/start")),
				"Should not send /start when stopping");
	}
}
