package io.openems.edge.meter.meshems.singlephase;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.channel.Level;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.test.DummyBridgeMqtt;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.meter.api.ElectricityMeter;

public class MeterMeshemsSinglePhaseImplTest {

	private static final String COMPONENT_ID = "meter0";
	private static final String MQTT_BRIDGE_ID = "mqtt0";
	private static final String TOPIC = "openami/StreetPoleEMS_37EAB0/meter_0";

	/** Confirmed real SunSpec model-11 payload from the MeshEMS OpenAMI board. */
	private static final String SAMPLE_PAYLOAD = "{\"model_id\":11,\"Phase\":0,\"Hz\":50.02,\"PhA\":2.834,"
			+ "\"PhV\":228.5,\"PhW\":616.7,\"VA\":647.7,\"Var\":198.2,\"PF\":0.952,"
			+ "\"TotWhImport\":100286.4,\"TotWhExport\":0}";

	@Test
	public void test() throws Exception {
		var mqttBridge = new DummyBridgeMqtt(MQTT_BRIDGE_ID);

		new ComponentTest(new MeterMeshemsSinglePhaseImpl()) //
				.addReference("mqttBridge", mqttBridge) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setMqttBridgeId(MQTT_BRIDGE_ID) //
						.setType(MeterType.CONSUMPTION_METERED) //
						.setPhase(SinglePhase.L1) //
						.setInvert(false) //
						.setTopic(TOPIC) //
						.build()) //
				.next(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> {
							assertTrue(mqttBridge.isSubscribed(TOPIC));
							mqttBridge.simulateMessage(TOPIC, SAMPLE_PAYLOAD);
						}) //
						// OpenemsComponent
						.output(OpenemsComponent.ChannelId.STATE, Level.OK) //
						// MqttComponent
						.output(MqttComponent.ChannelId.MQTT_COMMUNICATION_FAILED, false) //
						// ElectricityMeter - aggregates
						.output(ElectricityMeter.ChannelId.VOLTAGE, 228500) // V -> mV
						.output(ElectricityMeter.ChannelId.CURRENT, 2834) // A -> mA
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 617) // W
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER, 198) // var
						.output(ElectricityMeter.ChannelId.FREQUENCY, 50020) // Hz -> mHz
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 100286L) // Wh
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 0L) // Wh
						// ElectricityMeter - per-phase derived onto L1
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 228500) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 2834) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 617) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, 198) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, null) //
						// ElectricityMeter - per-phase energy not mapped -> null
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null)) //
				.deactivate();
	}
}
