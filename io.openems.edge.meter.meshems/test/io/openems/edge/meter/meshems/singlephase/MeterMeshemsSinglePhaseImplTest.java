package io.openems.edge.meter.meshems.singlephase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.channel.Level;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.test.DummyBridgeMqtt;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.meter.api.ElectricityMeter;

public class MeterMeshemsSinglePhaseImplTest {

	private static final String COMPONENT_ID = "meter0";
	private static final String MQTT_BRIDGE_ID = "mqtt0";
	private static final String TOPIC = "openami/StreetPoleEMS_37EAB0/meter_0";
	private static final int STALE_TIMEOUT_SECONDS = 90;

	/** Confirmed real SunSpec model-11 payload from the MeshEMS OpenAMI board. */
	private static final String SAMPLE_PAYLOAD = "{\"model_id\":11,\"Phase\":0,\"Hz\":50.02,\"PhA\":2.834,"
			+ "\"PhV\":228.5,\"PhW\":616.7,\"VA\":647.7,\"Var\":198.2,\"PF\":0.952,"
			+ "\"TotWhImport\":100286.4,\"TotWhExport\":0}";

	/** Same as {@link #SAMPLE_PAYLOAD} but without the {@code PhV} field. */
	private static final String PAYLOAD_WITHOUT_VOLTAGE = "{\"model_id\":11,\"Phase\":0,\"Hz\":50.02,\"PhA\":2.834,"
			+ "\"PhW\":616.7,\"VA\":647.7,\"Var\":198.2,\"PF\":0.952,"
			+ "\"TotWhImport\":100286.4,\"TotWhExport\":0}";

	/** A model-213 payload, i.e. the wrong SunSpec model for this component. */
	private static final String WRONG_MODEL_PAYLOAD = "{\"model_id\":213,\"Hz\":50.01,\"PhVphA\":230.0,"
			+ "\"TotWhImport\":1.0,\"TotWhExport\":2.0}";

	private static ComponentTest prepareTest(DummyBridgeMqtt mqttBridge, TimeLeapClock clock, boolean invert,
			String topic) throws Exception {
		return new ComponentTest(new MeterMeshemsSinglePhaseImpl()) //
				.addReference("setMqttBridge", mqttBridge) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setMqttBridgeId(MQTT_BRIDGE_ID) //
						.setType(MeterType.CONSUMPTION_METERED) //
						.setPhase(SinglePhase.L1) //
						.setInvert(invert) //
						.setTopic(topic) //
						.setStaleTimeoutSeconds(STALE_TIMEOUT_SECONDS) //
						.build());
	}

	private static TimeLeapClock createClock() {
		return new TimeLeapClock(Instant.parse("2026-01-01T00:00:00.00Z"), ZoneOffset.UTC);
	}

	// Declares the expected value of every Channel (required by Strict-Mode). The
	// per-phase values are expected on L1, the per-phase energies are never mapped.
	private static TestCase expect(TestCase testCase, Integer voltage, Integer current, Integer activePower,
			Integer reactivePower, Integer frequency, Long consumptionEnergy, Long productionEnergy, Level state,
			boolean mqttFailed) {
		return testCase //
				// OpenemsComponent
				.output(OpenemsComponent.ChannelId.STATE, state) //
				// MqttComponent
				.output(MqttComponent.ChannelId.MQTT_COMMUNICATION_FAILED, mqttFailed) //
				// ElectricityMeter - aggregates
				.output(ElectricityMeter.ChannelId.VOLTAGE, voltage) //
				.output(ElectricityMeter.ChannelId.CURRENT, current) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, activePower) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER, reactivePower) //
				.output(ElectricityMeter.ChannelId.FREQUENCY, frequency) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, consumptionEnergy) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, productionEnergy) //
				// ElectricityMeter - per-phase derived onto L1
				.output(ElectricityMeter.ChannelId.VOLTAGE_L1, voltage) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null) //
				.output(ElectricityMeter.ChannelId.CURRENT_L1, current) //
				.output(ElectricityMeter.ChannelId.CURRENT_L2, null) //
				.output(ElectricityMeter.ChannelId.CURRENT_L3, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, activePower) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, reactivePower) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, null) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, null) //
				// ElectricityMeter - per-phase energy not mapped -> null
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null);
	}

	private static TestCase expectSample(TestCase testCase) {
		return expect(testCase, 228500, 2834, 617, 198, 50020, 100286L, 0L, Level.OK, false);
	}

	private static TestCase expectCleared(TestCase testCase) {
		return expect(testCase, null, null, null, null, null, null, null, Level.WARNING, true);
	}

	@Test
	public void test() throws Exception {
		var mqttBridge = new DummyBridgeMqtt(MQTT_BRIDGE_ID);

		prepareTest(mqttBridge, createClock(), false, TOPIC) //
				.next(expectSample(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> {
							assertTrue(mqttBridge.isSubscribed(TOPIC));
							mqttBridge.simulateMessage(TOPIC, SAMPLE_PAYLOAD);
						}))) //
				.deactivate();
	}

	@Test
	public void testInvert() throws Exception {
		var mqttBridge = new DummyBridgeMqtt(MQTT_BRIDGE_ID);

		prepareTest(mqttBridge, createClock(), true, TOPIC) //
				.next(expect(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> mqttBridge.simulateMessage(TOPIC, SAMPLE_PAYLOAD)), //
						// Power negated, energies swapped
						228500, 2834, -617, -198, 50020, 0L, 100286L, Level.OK, false)) //
				.deactivate();
	}

	@Test
	public void testAbsentField() throws Exception {
		var mqttBridge = new DummyBridgeMqtt(MQTT_BRIDGE_ID);

		prepareTest(mqttBridge, createClock(), false, TOPIC) //
				.next(expect(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> mqttBridge.simulateMessage(TOPIC, PAYLOAD_WITHOUT_VOLTAGE)), //
						// PhV absent -> VOLTAGE undefined, everything else set
						null, 2834, 617, 198, 50020, 100286L, 0L, Level.OK, false)) //
				.deactivate();
	}

	@Test
	public void testMalformedPayload() throws Exception {
		var mqttBridge = new DummyBridgeMqtt(MQTT_BRIDGE_ID);

		prepareTest(mqttBridge, createClock(), false, TOPIC) //
				.next(expectSample(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> mqttBridge.simulateMessage(TOPIC, SAMPLE_PAYLOAD)))) //
				// Not JSON -> flagged, values unchanged
				.next(expect(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> mqttBridge.simulateMessage(TOPIC, "not json")), //
						228500, 2834, 617, 198, 50020, 100286L, 0L, Level.WARNING, true)) //
				// JSON but not an object -> flagged, values unchanged
				.next(expect(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> mqttBridge.simulateMessage(TOPIC, "[1,2,3]")), //
						228500, 2834, 617, 198, 50020, 100286L, 0L, Level.WARNING, true)) //
				// Valid payload restores
				.next(expectSample(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> mqttBridge.simulateMessage(TOPIC, SAMPLE_PAYLOAD)))) //
				.deactivate();
	}

	@Test
	public void testModelIdMismatch() throws Exception {
		var mqttBridge = new DummyBridgeMqtt(MQTT_BRIDGE_ID);

		prepareTest(mqttBridge, createClock(), false, TOPIC) //
				// Wrong model_id -> rejected, nothing applied
				.next(expectCleared(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> mqttBridge.simulateMessage(TOPIC, WRONG_MODEL_PAYLOAD)))) //
				// Matching payload accepted
				.next(expectSample(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> mqttBridge.simulateMessage(TOPIC, SAMPLE_PAYLOAD)))) //
				// Wrong model_id again -> rejected, previous values kept
				.next(expect(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> mqttBridge.simulateMessage(TOPIC, WRONG_MODEL_PAYLOAD)), //
						228500, 2834, 617, 198, 50020, 100286L, 0L, Level.WARNING, true)) //
				.deactivate();
	}

	@Test
	public void testStaleness() throws Exception {
		var mqttBridge = new DummyBridgeMqtt(MQTT_BRIDGE_ID);
		var clock = createClock();

		prepareTest(mqttBridge, clock, false, TOPIC) //
				.next(expectSample(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> mqttBridge.simulateMessage(TOPIC, SAMPLE_PAYLOAD)))) //
				// Within the stale timeout -> values kept
				.next(expectSample(new TestCase() //
						.activateStrictMode() //
						.timeleap(clock, STALE_TIMEOUT_SECONDS, ChronoUnit.SECONDS))) //
				// Past the stale timeout -> cleared and flagged; the cumulated energy
				// Channels keep their last total (OpenEMS ignores 'null' on them)
				.next(expect(new TestCase() //
						.activateStrictMode() //
						.timeleap(clock, 1, ChronoUnit.SECONDS), //
						null, null, null, null, null, 100286L, 0L, Level.WARNING, true)) //
				// New payload restores values and clears the flag
				.next(expectSample(new TestCase() //
						.activateStrictMode() //
						.timeleap(clock, 30, ChronoUnit.SECONDS) //
						.onBeforeProcessImage(() -> mqttBridge.simulateMessage(TOPIC, SAMPLE_PAYLOAD)))) //
				.deactivate();
	}

	@Test
	public void testValuesAppliedOnlyInCycle() throws Exception {
		var mqttBridge = new DummyBridgeMqtt(MQTT_BRIDGE_ID);

		prepareTest(mqttBridge, createClock(), false, TOPIC) //
				// Message arrives after the Cycle hook -> not visible yet
				.next(expectCleared(new TestCase() //
						.activateStrictMode() //
						.onAfterProcessImage(() -> mqttBridge.simulateMessage(TOPIC, SAMPLE_PAYLOAD)))) //
				// Next Cycle applies it
				.next(expectSample(new TestCase() //
						.activateStrictMode())) //
				.deactivate();
	}

	@Test
	public void testBlankTopic() throws Exception {
		var mqttBridge = new DummyBridgeMqtt(MQTT_BRIDGE_ID);

		prepareTest(mqttBridge, createClock(), false, " ") //
				.next(expectCleared(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> {
							assertFalse(mqttBridge.isSubscribed(" "));
							assertFalse(mqttBridge.isSubscribed(""));
						}))) //
				.deactivate();
	}
}
