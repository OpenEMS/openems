package io.openems.edge.evse.chargepoint.hardybarth.ecb1;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
import static io.openems.edge.common.test.TestUtils.withValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.channel.Level;
import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.bridge.http.cycle.dummy.DummyCycleSubscriber;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.type.Phase;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
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

	/** Chargecontrol response that should be forced back to manual mode. */
	private static final String CHARGECONTROL_AUTOMATIC_MODE = """
			{
			  "chargecontrol": {
			    "mode": "automatic",
			    "connected": true,
			    "manualmodeamp": 10.0,
			    "stateid": 17,
			    "currentpwmamp": 0.0,
			    "state": "B",
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

	/** Meter response without readable data. */
	private static final String METER_WITHOUT_DATA = """
			{
			  "meter": {
			    "serial": 75740051,
			    "vendor": "eCHARGE",
			    "type": "eCB1 intern"
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
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_STATE, "A") //
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_CONNECTED, false) //
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
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_STATE, "B") //
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_CONNECTED, true) //
				);
	}

	@Test
	void testCharging() throws Exception {
		var sut = new EvseChargePointHardyBarthEcb1Impl();
		var test = buildTest(sut);
		var handler = ReflectionUtils.<Ecb1Handler>getValueViaReflection(sut, "handler");
		test //
				.next(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> {
							handler.handleChargeControlResponse(CHARGECONTROL_CHARGING);
							handler.handleMeterResponse(METER_CHARGING);
						}) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true) //

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 11040) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 3680) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 3680) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 3680) //

						.output(ElectricityMeter.ChannelId.CURRENT, 48000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 16_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 16_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 16_000) //

						.output(ElectricityMeter.ChannelId.VOLTAGE, 230_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 230_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 230_000) //

						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 10000L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null) //
						.output(ElectricityMeter.ChannelId.FREQUENCY, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, null) //

						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_CONNECTED, true) //
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_CURRENT_PWM_AMP, 16.0) //
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_MANUAL_MODE_AMP, 16.0) //
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_METER_SERIAL, 75740051) //
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_METER_TYPE, "eCB1 intern") //
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_METER_VENDOR, "eCHARGE") //
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_MODE, "manual") //
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_STATE, "C") //
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_STATE_ID, 5) //
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_VENDOR, "Phoenix Contact") //
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_VERSION, "V1.3.1") //

						.output(OpenemsComponent.ChannelId.STATE, Level.OK) //
				);
	}

	@Test
	void testInvalidMeterResponseClearsPreviousValues() throws Exception {
		var sut = new EvseChargePointHardyBarthEcb1Impl();
		var test = buildTest(sut);
		var handler = ReflectionUtils.<Ecb1Handler>getValueViaReflection(sut, "handler");
		test //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> handler.handleMeterResponse(METER_CHARGING)) //
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_METER_VENDOR, "eCHARGE") //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 11040) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 16_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230_000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 10000L)) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> handler.handleMeterResponse(METER_WITHOUT_DATA)) //
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_METER_VENDOR, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, null));
	}

	@Test
	void testMalformedMeterResponseClearsPreviousValues() throws Exception {
		var sut = new EvseChargePointHardyBarthEcb1Impl();
		var test = buildTest(sut);
		var handler = ReflectionUtils.<Ecb1Handler>getValueViaReflection(sut, "handler");
		test //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> handler.handleMeterResponse(METER_CHARGING)) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 11040) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230_000)) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> handler.handleMeterResponse("{")) //
						.output(EvseChargePointHardyBarthEcb1.ChannelId.RAW_METER_SERIAL, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, null));
	}

	@Test
	void testMissingMeterClearsPreviousValues() throws Exception {
		var sut = new EvseChargePointHardyBarthEcb1Impl();
		var test = buildTest(sut);
		var handler = ReflectionUtils.<Ecb1Handler>getValueViaReflection(sut, "handler");
		test //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> handler.handleMeterResponse(METER_CHARGING)) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 11040) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 16_000)) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> handler.handleMeterResponse("{}")) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, null));
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
						.setMaxHwCurrent(32_000) //
						.build());

		// Simulate the controller calling apply() with 10 A
		var handler = ReflectionUtils.<Ecb1Handler>getValueViaReflection(sut, "handler");
		handler.setTarget(10);
		pool.update();

		assertTrue(sentUrls.stream().anyMatch(u -> u.contains("/mode/manual/ampere")), "Expected a manualmodeamp POST");
		assertTrue(sentUrls.stream().anyMatch(u -> u.contains("/start")), "Expected a start POST");
		assertTrue(sentBodies.stream().anyMatch(b -> b.contains("manualmodeamp=10")), "Expected manualmodeamp=10");
	}

	@Test
	void testSetCurrentSkipsDuplicateTarget() throws Exception {
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
						.setMaxHwCurrent(32_000) //
						.build());

		var handler = ReflectionUtils.<Ecb1Handler>getValueViaReflection(sut, "handler");
		handler.setTarget(10);
		pool.update();
		handler.setTarget(10);
		pool.update();

		assertEquals(1, sentUrls.stream().filter(u -> u.contains("/mode/manual/ampere")).count());
		assertEquals(1, sentUrls.stream().filter(u -> u.contains("/start")).count());
	}

	@Test
	void testAutomaticModeIsForcedBackToManual() throws Exception {
		final var pool = DummyBridgeHttpFactory.dummyBridgeHttpExecutor(false);
		final var httpBundle = DummyBridgeHttpBundle.of(pool);
		final var sentBodies = new java.util.ArrayList<String>();

		httpBundle.fetcher().addEndpointHandler(ep -> {
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
						.setMaxHwCurrent(32_000) //
						.build());

		sentBodies.clear();
		var handler = ReflectionUtils.<Ecb1Handler>getValueViaReflection(sut, "handler");
		handler.handleChargeControlResponse(CHARGECONTROL_AUTOMATIC_MODE);
		pool.update();

		assertTrue(sentBodies.stream().anyMatch(b -> b.contains("mode=manual")), "Expected mode=manual");
	}

	@Test
	void testGetChargePointAbilitiesDefaultThreePhase() throws Exception {
		var sut = new EvseChargePointHardyBarthEcb1Impl();
		buildTest(sut);

		var abilities = sut.getChargePointAbilities();
		var ampere = assertInstanceOf(ApplySetPoint.Ability.Ampere.class, abilities.applySetPoint());
		assertEquals(Phase.SingleOrThreePhase.THREE_PHASE, ampere.phase());
		assertEquals(6, ampere.min());
		assertEquals(32, ampere.max());
		assertFalse(abilities.isEvConnected());
	}

	@Test
	void testGetChargePointAbilitiesSinglePhase() throws Exception {
		var sut = new EvseChargePointHardyBarthEcb1Impl();
		buildTest(sut);
		withValue(sut, ElectricityMeter.ChannelId.CURRENT_L1, Evcs.MIN_EVCS_ACTIVITY_CURRENT + 1);

		var abilities = sut.getChargePointAbilities();
		var ampere = assertInstanceOf(ApplySetPoint.Ability.Ampere.class, abilities.applySetPoint());
		assertEquals(Phase.SingleOrThreePhase.SINGLE_PHASE, ampere.phase());
	}

	@Test
	void testGetChargePointAbilitiesReadOnly() throws Exception {
		var sut = new EvseChargePointHardyBarthEcb1Impl();
		new ComponentTest(sut) //
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
						.setMaxHwCurrent(32_000) //
						.setReadOnly(true) //
						.build());

		assertFalse(sut.getChargePointAbilities().applySetPoint() instanceof ApplySetPoint.Ability.Ampere);
	}

	@Test
	void testApplyCallsHandler() throws Exception {
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
						.setMaxHwCurrent(32_000) //
						.build());

		final var abilities = sut.getChargePointAbilities();
		sut.apply(ChargePointActions.from(abilities).setApplySetPointInAmpere(10).build());
		pool.update();

		assertTrue(sentUrls.stream().anyMatch(u -> u.contains("/start")), "Expected a /start POST via apply()");
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
						.setMaxHwCurrent(32_000) //
						.build());

		var handler = ReflectionUtils.<Ecb1Handler>getValueViaReflection(sut, "handler");
		handler.setTarget(0);
		pool.update();

		assertTrue(sentUrls.stream().anyMatch(u -> u.contains("/stop")), "Expected a stop POST");
		assertFalse(sentUrls.stream().anyMatch(u -> u.contains("/start")), "Should not send /start when stopping");
	}
}
