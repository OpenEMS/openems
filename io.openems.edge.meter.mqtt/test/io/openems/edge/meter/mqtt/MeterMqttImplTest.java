package io.openems.edge.meter.mqtt;

import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.FREQUENCY;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.REACTIVE_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.VOLTAGE;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.channel.Level;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.test.DummyBridgeMqtt;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.ElectricityMeter;

public class MeterMqttImplTest {

	private static final String COMPONENT_ID = "meter0";
	private static final String MQTT_BRIDGE_ID = "mqtt0";
	private static final String TOPIC = "openami/StreetPoleEMS_37EAB0/meter_0";

	private static final String[] MAPPING = { //
			"PhW:ACTIVE_POWER:1", //
			"PhV:VOLTAGE:1000", //
			"PhA:CURRENT:1000", //
			"Var:REACTIVE_POWER:1", //
			"Hz:FREQUENCY:1000", //
			"TotWhImport:ACTIVE_CONSUMPTION_ENERGY:1" //
	};

	private static final String SAMPLE_PAYLOAD = "{\"model_id\":11,\"Hz\":50.02,\"PhA\":2.83,\"PhV\":228.5,"
			+ "\"PhW\":616.7,\"Var\":198.2,\"TotWhImport\":100286.4}";

	@Test
	public void test() throws Exception {
		var mqttBridge = new DummyBridgeMqtt(MQTT_BRIDGE_ID);

		new ComponentTest(new MeterMqttImpl()) //
				.addReference("mqttBridge", mqttBridge) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setMqttBridgeId(MQTT_BRIDGE_ID) //
						.setType(MeterType.CONSUMPTION_METERED) //
						.setTopic(TOPIC) //
						.setMapping(MAPPING) //
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
						// ElectricityMeter - mapped by the sample payload
						.output(ACTIVE_POWER, 617) // W
						.output(VOLTAGE, 228500) // V -> mV
						.output(CURRENT, 2830) // A -> mA
						.output(REACTIVE_POWER, 198) // var
						.output(FREQUENCY, 50020) // Hz -> mHz
						.output(ACTIVE_CONSUMPTION_ENERGY, 100286L) // Wh (Long)
						// ElectricityMeter - not mapped -> null
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null)) //
				.deactivate();
	}
}
