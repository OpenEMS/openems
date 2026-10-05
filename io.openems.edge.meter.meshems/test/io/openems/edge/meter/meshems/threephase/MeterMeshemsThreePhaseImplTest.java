package io.openems.edge.meter.meshems.threephase;

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

public class MeterMeshemsThreePhaseImplTest {

	private static final String COMPONENT_ID = "meter0";
	private static final String MQTT_BRIDGE_ID = "mqtt0";
	private static final String TOPIC = "openami/StreetPoleEMS_37EAB0/subpanel_3Ph";

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

	@Test
	public void test() throws Exception {
		var mqttBridge = new DummyBridgeMqtt(MQTT_BRIDGE_ID);

		new ComponentTest(new MeterMeshemsThreePhaseImpl()) //
				.addReference("mqttBridge", mqttBridge) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setMqttBridgeId(MQTT_BRIDGE_ID) //
						.setType(MeterType.CONSUMPTION_METERED) //
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
						// ElectricityMeter - per-phase mapped from model 213
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230000) // V -> mV
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 231000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 229000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 2500) // A -> mA
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 3000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 1500) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 575) // W
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 672) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 345) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, 100) // var
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, 120) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, 80) //
						// ElectricityMeter - aggregates derived from phases
						.output(ElectricityMeter.ChannelId.VOLTAGE, 230000) // average
						.output(ElectricityMeter.ChannelId.CURRENT, 7000) // sum
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 1592) // sum
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER, 300) // sum
						.output(ElectricityMeter.ChannelId.FREQUENCY, 50010) // Hz -> mHz
						// ElectricityMeter - energy totals
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 123456L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 654L) //
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
