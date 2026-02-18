package io.openems.edge.evcc.loadpoint;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.JsonParser;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.bridge.http.cycle.dummy.DummyCycleSubscriber;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.SocEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Tests for LoadpointConsumptionMeterEvccImpl using real EVCC API response structures.
 * Test data is anonymized/generalized from actual EVCC installations.
 */
public class LoadpointConsumptionMeterEvccImplTest {

	private static final String COMPONENT_ID = "evcc0";

	/**
	 * Real scenario: Vehicle connected but not charging (PV mode, waiting for sun).
	 * Based on actual EVCC response - no chargeVoltages, idle currents.
	 */
	@Test
	public void testConnectedNotCharging() throws Exception {
		final var sut = new LoadpointConsumptionMeterEvccImpl();

		new ComponentTest(sut)
				.addReference("httpBridgeFactory", ofBridgeImpl(() -> dummyEndpointFetcher(), () -> dummyBridgeHttpExecutor()))
				.addReference("httpBridgeCycleServiceDefinition", new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber()))
				.activate(MyConfig.create()
						.setId(COMPONENT_ID)
						.setApiUrl("http://evcc:7070/api/state")
						.setLoadpointTitle("Loadpoint_1")
						.build());

		// Real EVCC response structure - connected but not charging
		final String json = """
				{
				  "title": "Loadpoint_1",
				  "mode": "pv",
				  "charging": false,
				  "connected": true,
				  "enabled": false,
				  "chargePower": 0,
				  "chargeCurrents": [0.008, 0.008, 0.008],
				  "chargeTotalImport": 5804.405,
				  "chargedEnergy": 0,
				  "sessionEnergy": 0,
				  "phasesActive": 1,
				  "vehicleSoc": 77.27,
				  "vehicleName": "EV",
				  "vehicleRange": 280
				}
				""";

		var jsonElement = JsonParser.parseString(json);
		sut.processHttpResult(HttpResponse.ok(jsonElement), null);

		// Verify parsing - enums are stored as integers
		assertEquals(Integer.valueOf(0), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER).getNextValue().get());
		assertEquals(Integer.valueOf(PlugState.CONNECTED.getValue()), sut.channel(LoadpointConsumptionMeterEvcc.ChannelId.PLUG).getNextValue().get());
		assertEquals(Integer.valueOf(Status.READY_FOR_CHARGING.getValue()), sut.channel(Evcs.ChannelId.STATUS).getNextValue().get());
		assertEquals(Integer.valueOf(77), sut.channel(SocEvcs.ChannelId.SOC).getNextValue().get()); // Rounded
		assertEquals("EV", sut.channel(LoadpointConsumptionMeterEvcc.ChannelId.VEHICLE_NAME).getNextValue().get());
		assertEquals(Integer.valueOf(1), sut.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_PHASES).getNextValue().get());

		// Idle currents should be parsed (in mA)
		assertEquals(Integer.valueOf(8), sut.channel(ElectricityMeter.ChannelId.CURRENT_L1).getNextValue().get());

		// No chargeVoltages in response - should default to 230V
		assertEquals(Integer.valueOf(230000), sut.channel(ElectricityMeter.ChannelId.VOLTAGE_L1).getNextValue().get());
	}

	/**
	 * Scenario: Active 3-phase charging with full data.
	 */
	@Test
	public void testActiveCharging3Phase() throws Exception {
		final var sut = new LoadpointConsumptionMeterEvccImpl();

		new ComponentTest(sut)
				.addReference("httpBridgeFactory", ofBridgeImpl(() -> dummyEndpointFetcher(), () -> dummyBridgeHttpExecutor()))
				.addReference("httpBridgeCycleServiceDefinition", new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber()))
				.activate(MyConfig.create()
						.setId(COMPONENT_ID)
						.setApiUrl("http://evcc:7070/api/state")
						.build());

		final String json = """
				{
				  "title": "Loadpoint_1",
				  "mode": "now",
				  "charging": true,
				  "connected": true,
				  "enabled": true,
				  "chargePower": 11040,
				  "chargeCurrents": [16.1, 15.9, 16.0],
				  "chargeVoltages": [230.5, 229.8, 231.2],
				  "chargeTotalImport": 12847.123,
				  "sessionEnergy": 8420,
				  "phasesActive": 3,
				  "vehicleSoc": 42,
				  "vehicleName": "EV"
				}
				""";

		var jsonElement = JsonParser.parseString(json);
		sut.processHttpResult(HttpResponse.ok(jsonElement), null);

		assertEquals(Integer.valueOf(11040), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER).getNextValue().get());
		assertEquals(Integer.valueOf(Status.CHARGING.getValue()), sut.channel(Evcs.ChannelId.STATUS).getNextValue().get());
		assertEquals(Integer.valueOf(3), sut.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_PHASES).getNextValue().get());
		assertEquals(Integer.valueOf(8420), sut.channel(Evcs.ChannelId.ENERGY_SESSION).getNextValue().get());

		// Voltages in mV
		assertEquals(Integer.valueOf(230500), sut.channel(ElectricityMeter.ChannelId.VOLTAGE_L1).getNextValue().get());
		assertEquals(Integer.valueOf(229800), sut.channel(ElectricityMeter.ChannelId.VOLTAGE_L2).getNextValue().get());
		assertEquals(Integer.valueOf(231200), sut.channel(ElectricityMeter.ChannelId.VOLTAGE_L3).getNextValue().get());

		// Currents in mA
		assertEquals(Integer.valueOf(16100), sut.channel(ElectricityMeter.ChannelId.CURRENT_L1).getNextValue().get());
		assertEquals(Integer.valueOf(15900), sut.channel(ElectricityMeter.ChannelId.CURRENT_L2).getNextValue().get());
		assertEquals(Integer.valueOf(16000), sut.channel(ElectricityMeter.ChannelId.CURRENT_L3).getNextValue().get());

		// Verify per-phase power is calculated from V*I (not summed back to ACTIVE_POWER)
		// L1: 230500mV * 16100mA / 1000000 = 3711W
		// L2: 229800mV * 15900mA / 1000000 = 3653W
		// L3: 231200mV * 16000mA / 1000000 = 3699W
		// Sum would be ~11063W, but ACTIVE_POWER should remain 11040 from chargePower
		assertEquals(Integer.valueOf(3711), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L1).getNextValue().get());
		assertEquals(Integer.valueOf(3653), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L2).getNextValue().get());
		assertEquals(Integer.valueOf(3699), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L3).getNextValue().get());
	}

	/**
	 * Real-world scenario: 3-phase charging with currents but NO voltages.
	 * This is the most common case - many chargers report currents but not voltages.
	 * Based on actual EVCC API response structure.
	 */
	@Test
	public void testChargingWithCurrentsNoVoltages() throws Exception {
		final var sut = new LoadpointConsumptionMeterEvccImpl();

		new ComponentTest(sut)
				.addReference("httpBridgeFactory", ofBridgeImpl(() -> dummyEndpointFetcher(), () -> dummyBridgeHttpExecutor()))
				.addReference("httpBridgeCycleServiceDefinition", new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber()))
				.activate(MyConfig.create()
						.setId(COMPONENT_ID)
						.setApiUrl("http://evcc:7070/api/state")
						.build());

		// Real-world scenario: 11kW 3-phase charging with currents but no voltages
		// This matches actual EVCC API responses from real installations
		final String json = """
				{
				  "title": "Loadpoint_1",
				  "charging": true,
				  "connected": true,
				  "chargePower": 11040,
				  "chargeCurrents": [16.0, 16.0, 16.0],
				  "phasesActive": 3,
				  "vehicleSoc": 45
				}
				""";

		sut.processHttpResult(HttpResponse.ok(JsonParser.parseString(json)), null);

		// Power from EVCC
		assertEquals(Integer.valueOf(11040), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER).getNextValue().get());

		// Voltages default to 230V (230000mV) when not provided
		assertEquals(Integer.valueOf(230000), sut.channel(ElectricityMeter.ChannelId.VOLTAGE_L1).getNextValue().get());
		assertEquals(Integer.valueOf(230000), sut.channel(ElectricityMeter.ChannelId.VOLTAGE_L2).getNextValue().get());
		assertEquals(Integer.valueOf(230000), sut.channel(ElectricityMeter.ChannelId.VOLTAGE_L3).getNextValue().get());

		// Currents from API (in mA)
		assertEquals(Integer.valueOf(16000), sut.channel(ElectricityMeter.ChannelId.CURRENT_L1).getNextValue().get());
		assertEquals(Integer.valueOf(16000), sut.channel(ElectricityMeter.ChannelId.CURRENT_L2).getNextValue().get());
		assertEquals(Integer.valueOf(16000), sut.channel(ElectricityMeter.ChannelId.CURRENT_L3).getNextValue().get());

		// Per-phase power from V*I: 230000mV * 16000mA / 1000000 = 3680W
		assertEquals(Integer.valueOf(3680), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L1).getNextValue().get());
		assertEquals(Integer.valueOf(3680), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L2).getNextValue().get());
		assertEquals(Integer.valueOf(3680), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L3).getNextValue().get());

		// Note: Sum of per-phase power (3*3680=11040W) matches chargePower in this case
		// because we're using default voltage (230V) and currents are balanced
	}

	/**
	 * Validate ACTIVE_POWER is taken directly from chargePower, not calculated.
	 * Tests various power levels with full voltage/current data.
	 * Note: This is a synthetic test - real EVCC may not always provide chargeVoltages.
	 */
	@Test
	public void testVariousPowerLevels() throws Exception {
		final var sut = new LoadpointConsumptionMeterEvccImpl();

		new ComponentTest(sut)
				.addReference("httpBridgeFactory", ofBridgeImpl(() -> dummyEndpointFetcher(), () -> dummyBridgeHttpExecutor()))
				.addReference("httpBridgeCycleServiceDefinition", new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber()))
				.activate(MyConfig.create()
						.setId(COMPONENT_ID)
						.setApiUrl("http://evcc:7070/api/state")
						.build());

		// Test 1.4kW single-phase charging
		String json = """
				{
				  "title": "Loadpoint_1",
				  "charging": true,
				  "connected": true,
				  "chargePower": 1400,
				  "chargeCurrents": [6.1, 0, 0],
				  "chargeVoltages": [230, 0, 0],
				  "phasesActive": 1
				}
				""";
		sut.processHttpResult(HttpResponse.ok(JsonParser.parseString(json)), null);
		assertEquals(Integer.valueOf(1400), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER).getNextValue().get());
		// L1 power from V*I: 230000mV * 6100mA / 1000000 = 1403W
		assertEquals(Integer.valueOf(1403), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L1).getNextValue().get());

		// Test 7.2kW 3-phase charging (balanced)
		json = """
				{
				  "title": "Loadpoint_1",
				  "charging": true,
				  "connected": true,
				  "chargePower": 7200,
				  "chargeCurrents": [10.5, 10.4, 10.5],
				  "chargeVoltages": [229, 230, 229],
				  "phasesActive": 3
				}
				""";
		sut.processHttpResult(HttpResponse.ok(JsonParser.parseString(json)), null);
		assertEquals(Integer.valueOf(7200), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER).getNextValue().get());
		// Per-phase powers from V*I
		assertEquals(Integer.valueOf(2404), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L1).getNextValue().get()); // 229000*10500/1e6
		assertEquals(Integer.valueOf(2392), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L2).getNextValue().get()); // 230000*10400/1e6
		assertEquals(Integer.valueOf(2404), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L3).getNextValue().get()); // 229000*10500/1e6

		// Test 4.1kW 2-phase charging (unbalanced)
		json = """
				{
				  "title": "Loadpoint_1",
				  "charging": true,
				  "connected": true,
				  "chargePower": 4100,
				  "chargeCurrents": [8.9, 8.9, 0],
				  "chargeVoltages": [230, 231, 0],
				  "phasesActive": 2
				}
				""";
		sut.processHttpResult(HttpResponse.ok(JsonParser.parseString(json)), null);
		assertEquals(Integer.valueOf(4100), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER).getNextValue().get());
		// Per-phase powers from V*I
		assertEquals(Integer.valueOf(2047), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L1).getNextValue().get()); // 230000*8900/1e6
		assertEquals(Integer.valueOf(2055), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L2).getNextValue().get()); // 231000*8900/1e6
		// L3 should be null (phases > 2 is false)
		assertTrue(sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L3).getNextValue().asOptional().isEmpty());
	}

	/**
	 * Scenario: Single-phase charging without voltage/current data (e-bike charger style).
	 * Tests current estimation from power when EVCC doesn't provide chargeCurrents/chargeVoltages.
	 * Based on real eBike loadpoint structure.
	 */
	@Test
	public void testSinglePhaseCharging() throws Exception {
		final var sut = new LoadpointConsumptionMeterEvccImpl();

		new ComponentTest(sut)
				.addReference("httpBridgeFactory", ofBridgeImpl(() -> dummyEndpointFetcher(), () -> dummyBridgeHttpExecutor()))
				.addReference("httpBridgeCycleServiceDefinition", new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber()))
				.activate(MyConfig.create()
						.setId(COMPONENT_ID)
						.setApiUrl("http://evcc:7070/api/state")
						.build());

		// Minimal response like eBike charger - no currents/voltages arrays
		// This is realistic - simple chargers don't report per-phase data
		final String json = """
				{
				  "title": "Loadpoint_2",
				  "mode": "pv",
				  "charging": true,
				  "connected": true,
				  "enabled": true,
				  "chargePower": 46,
				  "chargeTotalImport": 24.831,
				  "sessionEnergy": 12,
				  "phasesActive": 1,
				  "vehicleName": "eBike"
				}
				""";

		var jsonElement = JsonParser.parseString(json);
		sut.processHttpResult(HttpResponse.ok(jsonElement), null);

		assertEquals(Integer.valueOf(46), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER).getNextValue().get());
		assertEquals(Integer.valueOf(Status.CHARGING.getValue()), sut.channel(Evcs.ChannelId.STATUS).getNextValue().get());
		assertEquals(Integer.valueOf(1), sut.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_PHASES).getNextValue().get());

		// No vehicleSoc for eBike
		assertTrue(sut.channel(SocEvcs.ChannelId.SOC).getNextValue().asOptional().isEmpty());

		// Voltage should default to 230V (230000mV) when not provided
		assertEquals(Integer.valueOf(230000), sut.channel(ElectricityMeter.ChannelId.VOLTAGE_L1).getNextValue().get());

		// Current should be estimated from power: 46W * 1000000 / 230000mV = 200mA
		assertEquals(Integer.valueOf(200), sut.channel(ElectricityMeter.ChannelId.CURRENT_L1).getNextValue().get());

		// Per-phase power L1 from V*I: 230000mV * 200mA / 1000000 = 46W
		assertEquals(Integer.valueOf(46), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L1).getNextValue().get());
	}

	/**
	 * Scenario: No vehicle connected.
	 */
	@Test
	public void testDisconnected() throws Exception {
		final var sut = new LoadpointConsumptionMeterEvccImpl();

		new ComponentTest(sut)
				.addReference("httpBridgeFactory", ofBridgeImpl(() -> dummyEndpointFetcher(), () -> dummyBridgeHttpExecutor()))
				.addReference("httpBridgeCycleServiceDefinition", new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber()))
				.activate(MyConfig.create()
						.setId(COMPONENT_ID)
						.setApiUrl("http://evcc:7070/api/state")
						.build());

		final String json = """
				{
				  "title": "Loadpoint_1",
				  "mode": "off",
				  "charging": false,
				  "connected": false,
				  "enabled": false,
				  "chargePower": 0,
				  "chargeTotalImport": 5804.405,
				  "sessionEnergy": 0,
				  "phasesActive": 0
				}
				""";

		var jsonElement = JsonParser.parseString(json);
		sut.processHttpResult(HttpResponse.ok(jsonElement), null);

		assertEquals(Integer.valueOf(0), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER).getNextValue().get());
		assertEquals(Integer.valueOf(PlugState.UNPLUGGED.getValue()), sut.channel(LoadpointConsumptionMeterEvcc.ChannelId.PLUG).getNextValue().get());
		assertEquals(Integer.valueOf(Status.NOT_READY_FOR_CHARGING.getValue()), sut.channel(Evcs.ChannelId.STATUS).getNextValue().get());

		// Verify chargeTotalImport is stored in dedicated channel
		assertEquals(Long.valueOf(5804405), sut.channel(LoadpointConsumptionMeterEvcc.ChannelId.CHARGE_TOTAL_IMPORT).getNextValue().get());

		// No vehicle info when disconnected
		assertTrue(sut.channel(SocEvcs.ChannelId.SOC).getNextValue().asOptional().isEmpty());
		assertTrue(sut.channel(LoadpointConsumptionMeterEvcc.ChannelId.VEHICLE_NAME).getNextValue().asOptional().isEmpty());
	}

	/**
	 * Scenario: HTTP communication error.
	 */
	@Test
	public void testCommunicationError() throws Exception {
		final var sut = new LoadpointConsumptionMeterEvccImpl();

		new ComponentTest(sut)
				.addReference("httpBridgeFactory", ofBridgeImpl(() -> dummyEndpointFetcher(), () -> dummyBridgeHttpExecutor()))
				.addReference("httpBridgeCycleServiceDefinition", new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber()))
				.activate(MyConfig.create()
						.setId(COMPONENT_ID)
						.setApiUrl("http://evcc:7070/api/state")
						.build());

		sut.processHttpResult(null, HttpError.ResponseError.notFound());

		assertEquals(Boolean.TRUE, sut.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).getNextValue().get());
	}

	/**
	 * Scenario: phasesActive=0 should default to 3 phases for 3-phase meter.
	 */
	@Test
	public void testPhasesActiveZeroDefaults() throws Exception {
		final var sut = new LoadpointConsumptionMeterEvccImpl();

		new ComponentTest(sut)
				.addReference("httpBridgeFactory", ofBridgeImpl(() -> dummyEndpointFetcher(), () -> dummyBridgeHttpExecutor()))
				.addReference("httpBridgeCycleServiceDefinition", new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber()))
				.activate(MyConfig.create()
						.setId(COMPONENT_ID)
						.setApiUrl("http://evcc:7070/api/state")
						.build());

		final String json = """
				{
				  "title": "Test",
				  "charging": false,
				  "connected": false,
				  "chargePower": 0,
				  "phasesActive": 0
				}
				""";

		var jsonElement = JsonParser.parseString(json);
		sut.processHttpResult(HttpResponse.ok(jsonElement), null);

		// phasesActive=0 should set Evcs.PHASES to 3 (default for 3-phase meter)
		assertEquals(Integer.valueOf(3), sut.channel(Evcs.ChannelId.PHASES).getNextValue().get());
	}

	@Test
	public void testActivation() throws Exception {
		new ComponentTest(new LoadpointConsumptionMeterEvccImpl())
				.addReference("httpBridgeFactory", ofBridgeImpl(() -> dummyEndpointFetcher(), () -> dummyBridgeHttpExecutor()))
				.addReference("httpBridgeCycleServiceDefinition", new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber()))
				.activate(MyConfig.create()
						.setId(COMPONENT_ID)
						.setApiUrl("http://evcc:7070/api/state")
						.build());
	}

	/**
	 * Test with real production JSON from actual EVCC installation.
	 * This is the exact response structure from a real EV charging session.
	 */
	@Test
	public void testRealProductionJsonCharging() throws Exception {
		final var sut = new LoadpointConsumptionMeterEvccImpl();

		new ComponentTest(sut)
				.addReference("httpBridgeFactory", ofBridgeImpl(() -> dummyEndpointFetcher(), () -> dummyBridgeHttpExecutor()))
				.addReference("httpBridgeCycleServiceDefinition", new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber()))
				.activate(MyConfig.create()
						.setId(COMPONENT_ID)
						.setApiUrl("http://evcc:7070/api/state")
						.setLoadpointTitle("Loadpoint_1")
						.build());

		// Real production JSON - 2-phase charging at 7.2kW
		final String json = """
				{
				  "title": "Loadpoint_1",
				  "mode": "now",
				  "charging": true,
				  "connected": true,
				  "enabled": true,
				  "chargePower": 7253,
				  "chargeCurrents": [15.661999702453613, 15.423999786376953, 0.01899999938905239],
				  "chargeTotalImport": 5804.405,
				  "chargedEnergy": 0,
				  "sessionEnergy": 0,
				  "phasesActive": 2,
				  "vehicleSoc": 77.27,
				  "vehicleName": "EV",
				  "vehicleRange": 0
				}
				""";

		var jsonElement = JsonParser.parseString(json);
		sut.processHttpResult(HttpResponse.ok(jsonElement), null);

		// Verify power
		assertEquals(Integer.valueOf(7253), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER).getNextValue().get());

		// Verify status
		assertEquals(Integer.valueOf(Status.CHARGING.getValue()), sut.channel(Evcs.ChannelId.STATUS).getNextValue().get());
		assertEquals(Integer.valueOf(PlugState.CONNECTED.getValue()), sut.channel(LoadpointConsumptionMeterEvcc.ChannelId.PLUG).getNextValue().get());

		// Verify phases
		// ACTIVE_PHASES = currently used phases (vehicle limitation)
		// EVCS.PHASES = hardware configuration (Loadpoint_1 is 3-phase, defaults to 3 if phasesActive > 0)
		assertEquals(Integer.valueOf(2), sut.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_PHASES).getNextValue().get());

		// Verify vehicle info
		assertEquals(Integer.valueOf(77), sut.channel(SocEvcs.ChannelId.SOC).getNextValue().get());
		assertEquals("EV", sut.channel(LoadpointConsumptionMeterEvcc.ChannelId.VEHICLE_NAME).getNextValue().get());

		// Verify currents (in mA) - 2-phase charging
		assertEquals(Integer.valueOf(15662), sut.channel(ElectricityMeter.ChannelId.CURRENT_L1).getNextValue().get());
		assertEquals(Integer.valueOf(15424), sut.channel(ElectricityMeter.ChannelId.CURRENT_L2).getNextValue().get());
		assertEquals(Integer.valueOf(19), sut.channel(ElectricityMeter.ChannelId.CURRENT_L3).getNextValue().get());

		// Verify voltages default to 230V when not provided
		assertEquals(Integer.valueOf(230000), sut.channel(ElectricityMeter.ChannelId.VOLTAGE_L1).getNextValue().get());
		assertEquals(Integer.valueOf(230000), sut.channel(ElectricityMeter.ChannelId.VOLTAGE_L2).getNextValue().get());
		assertEquals(Integer.valueOf(230000), sut.channel(ElectricityMeter.ChannelId.VOLTAGE_L3).getNextValue().get());

		// Verify per-phase power (only L1 and L2 for 2-phase)
		// L1: 230000mV * 15662mA / 1000000 = 3602W
		// L2: 230000mV * 15424mA / 1000000 = 3547W
		assertEquals(Integer.valueOf(3602), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L1).getNextValue().get());
		assertEquals(Integer.valueOf(3547), sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L2).getNextValue().get());
		// L3 should be null (phases > 2 is false)
		assertTrue(sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L3).getNextValue().asOptional().isEmpty());
	}
}
