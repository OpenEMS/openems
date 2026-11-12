package io.openems.edge.meter.meshems.threephase;

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
import io.openems.edge.meter.api.ElectricityMeter;

public class MeterMeshemsThreePhaseImplTest {

	private static final String COMPONENT_ID = "meter0";
	private static final String MQTT_BRIDGE_ID = "mqtt0";
	private static final String TOPIC = "openami/StreetPoleEMS_37EAB0/subpanel_3Ph";
	private static final int STALE_TIMEOUT_SECONDS = 90;

	/**
	 * Representative SunSpec model-213 payload using the real field names emitted by
	 * {@code SunSpecModel213::toJson} in the MeshEMS OpenAMI firmware
	 * (include/metering/sunspec_model_213.h).
	 */
	private static final String SAMPLE_PAYLOAD = "{\"model_id\":213,\"length\":85,\"Hz\":50.01,"
			+ "\"AphA\":2.5,\"AphB\":3.0,\"AphC\":1.5,"
			+ "\"PhVphA\":230.0,\"PhVphB\":231.0,\"PhVphC\":229.0,"
			+ "\"PFphA\":0.95,\"PFphB\":0.96,\"PFphC\":0.94,"
			+ "\"VarphA\":100.0,\"VarphB\":120.0,\"VarphC\":80.0,"
			+ "\"VAphA\":600.0,\"VAphB\":700.0,\"VAphC\":350.0,"
			+ "\"WphA\":575.0,\"WphB\":672.0,\"WphC\":345.0,"
			+ "\"TotWhImport\":123456.0,\"TotWhExport\":654.0}";

	/** Same as {@link #SAMPLE_PAYLOAD} but without the {@code Hz} field. */
	private static final String PAYLOAD_WITHOUT_FREQUENCY = "{\"model_id\":213,\"length\":85,"
			+ "\"AphA\":2.5,\"AphB\":3.0,\"AphC\":1.5,"
			+ "\"PhVphA\":230.0,\"PhVphB\":231.0,\"PhVphC\":229.0,"
			+ "\"VarphA\":100.0,\"VarphB\":120.0,\"VarphC\":80.0,"
			+ "\"WphA\":575.0,\"WphB\":672.0,\"WphC\":345.0,"
			+ "\"TotWhImport\":123456.0,\"TotWhExport\":654.0}";

	/** A model-11 payload, i.e. the wrong SunSpec model for this component. */
	private static final String WRONG_MODEL_PAYLOAD = "{\"model_id\":11,\"Hz\":50.02,\"PhV\":228.5,"
			+ "\"TotWhImport\":1.0,\"TotWhExport\":2.0}";

	private static ComponentTest prepareTest(DummyBridgeMqtt mqttBridge, TimeLeapClock clock, boolean invert,
			String topic) throws Exception {
		return new ComponentTest(new MeterMeshemsThreePhaseImpl()) //
				.addReference("setMqttBridge", mqttBridge) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setMqttBridgeId(MQTT_BRIDGE_ID) //
						.setType(MeterType.CONSUMPTION_METERED) //
						.setInvert(invert) //
						.setTopic(topic) //
						.setStaleTimeoutSeconds(STALE_TIMEOUT_SECONDS) //
						.build());
	}

	private static TimeLeapClock createClock() {
		return new TimeLeapClock(Instant.parse("2026-01-01T00:00:00.00Z"), ZoneOffset.UTC);
	}

	// Declares the expected value of every Channel (required by Strict-Mode). The
	// 'sign' is applied to the sample power values (1 or -1), the aggregates are
	// derived from the phases, the per-phase energies are never mapped.
	private static TestCase expect(TestCase testCase, boolean hasValues, int sign, Integer frequency,
			Long consumptionEnergy, Long productionEnergy, Level state, boolean mqttFailed) {
		return testCase //
				// OpenemsComponent
				.output(OpenemsComponent.ChannelId.STATE, state) //
				// MqttComponent
				.output(MqttComponent.ChannelId.MQTT_COMMUNICATION_FAILED, mqttFailed) //
				// ElectricityMeter - per-phase mapped from model 213
				.output(ElectricityMeter.ChannelId.VOLTAGE_L1, hasValues ? 230000 : null) // V -> mV
				.output(ElectricityMeter.ChannelId.VOLTAGE_L2, hasValues ? 231000 : null) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L3, hasValues ? 229000 : null) //
				.output(ElectricityMeter.ChannelId.CURRENT_L1, hasValues ? 2500 : null) // A -> mA
				.output(ElectricityMeter.ChannelId.CURRENT_L2, hasValues ? 3000 : null) //
				.output(ElectricityMeter.ChannelId.CURRENT_L3, hasValues ? 1500 : null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, hasValues ? sign * 575 : null) // W
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, hasValues ? sign * 672 : null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, hasValues ? sign * 345 : null) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, hasValues ? sign * 100 : null) // var
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, hasValues ? sign * 120 : null) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, hasValues ? sign * 80 : null) //
				// ElectricityMeter - aggregates derived from phases
				.output(ElectricityMeter.ChannelId.VOLTAGE, hasValues ? 230000 : null) // average
				.output(ElectricityMeter.ChannelId.CURRENT, hasValues ? 7000 : null) // sum
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, hasValues ? sign * 1592 : null) // sum
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER, hasValues ? sign * 300 : null) // sum
				.output(ElectricityMeter.ChannelId.FREQUENCY, frequency) // Hz -> mHz
				// ElectricityMeter - energy totals
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, consumptionEnergy) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, productionEnergy) //
				// ElectricityMeter - per-phase energy not mapped -> null
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null);
	}

	private static TestCase expectSample(TestCase testCase) {
		return expect(testCase, true, 1, 50010, 123456L, 654L, Level.OK, false);
	}

	private static TestCase expectSampleFlagged(TestCase testCase) {
		return expect(testCase, true, 1, 50010, 123456L, 654L, Level.WARNING, true);
	}

	private static TestCase expectCleared(TestCase testCase) {
		return expect(testCase, false, 1, null, null, null, Level.WARNING, true);
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
						true, -1, 50010, 654L, 123456L, Level.OK, false)) //
				.deactivate();
	}

	@Test
	public void testAbsentField() throws Exception {
		var mqttBridge = new DummyBridgeMqtt(MQTT_BRIDGE_ID);

		prepareTest(mqttBridge, createClock(), false, TOPIC) //
				.next(expect(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> mqttBridge.simulateMessage(TOPIC, PAYLOAD_WITHOUT_FREQUENCY)), //
						// Hz absent -> FREQUENCY undefined, everything else set
						true, 1, null, 123456L, 654L, Level.OK, false)) //
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
				.next(expectSampleFlagged(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> mqttBridge.simulateMessage(TOPIC, "not json")))) //
				// JSON but not an object -> flagged, values unchanged
				.next(expectSampleFlagged(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> mqttBridge.simulateMessage(TOPIC, "[1,2,3]")))) //
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
				.next(expectSampleFlagged(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> mqttBridge.simulateMessage(TOPIC, WRONG_MODEL_PAYLOAD)))) //
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
						false, 1, null, 123456L, 654L, Level.WARNING, true)) //
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
