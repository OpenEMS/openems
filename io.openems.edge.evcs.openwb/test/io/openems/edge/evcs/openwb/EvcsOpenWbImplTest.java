package io.openems.edge.evcs.openwb;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.bridge.mqtt.test.DummyBridgeMqtt;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvcsOpenWbImplTest {

	private static final String COMPONENT_ID = "evcs0";
	private static final String MQTT_BRIDGE_ID = "mqtt0";

	@Test
	public void test() throws Exception {
		var mqttBridge = new DummyBridgeMqtt(MQTT_BRIDGE_ID);

		new ComponentTest(new EvcsOpenWbImpl()) //
				.addReference("setMqttBridge", mqttBridge) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setMqttBridgeId(MQTT_BRIDGE_ID) //
						.setChargePoint(ChargePoint.CP0) //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							// Verify subscription was created
							assertTrue(mqttBridge.isSubscribed("openWB/internal_chargepoint/0/get/#"));
						})) //
				.deactivate();
	}

	@Test
	public void testMqttMessages() throws Exception {
		var mqttBridge = new DummyBridgeMqtt(MQTT_BRIDGE_ID);

		var sut = new EvcsOpenWbImpl();
		new ComponentTest(sut) //
				.addReference("setMqttBridge", mqttBridge) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setMqttBridgeId(MQTT_BRIDGE_ID) //
						.setChargePoint(ChargePoint.CP0) //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							// Simulate power message
							mqttBridge.simulateMessage("openWB/internal_chargepoint/0/get/power", "3500");
						}) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 3500)) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							// Simulate voltages array
							mqttBridge.simulateMessage("openWB/internal_chargepoint/0/get/voltages",
									"[230.5, 231.2, 229.8]");
						}) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230500) // V to mV
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 231200) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 229800)) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							// Simulate currents array
							mqttBridge.simulateMessage("openWB/internal_chargepoint/0/get/currents",
									"[16.0, 15.8, 16.2]");
						}) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 16000) // A to mA
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 15800) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 16200)) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							// Simulate plug state
							mqttBridge.simulateMessage("openWB/internal_chargepoint/0/get/plug_state", "true");
							mqttBridge.simulateMessage("openWB/internal_chargepoint/0/get/charge_state", "false");
						}) //
						.output(Evcs.ChannelId.STATUS, Status.READY_FOR_CHARGING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							// Simulate charging state
							mqttBridge.simulateMessage("openWB/internal_chargepoint/0/get/charge_state", "true");
						}) //
						.output(Evcs.ChannelId.STATUS, Status.CHARGING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							// Simulate energy imported
							mqttBridge.simulateMessage("openWB/internal_chargepoint/0/get/imported", "12345678");
						}) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 12345678L)) //
				.deactivate();
	}

	@Test
	public void testChargePointCP1() throws Exception {
		var mqttBridge = new DummyBridgeMqtt(MQTT_BRIDGE_ID);

		new ComponentTest(new EvcsOpenWbImpl()) //
				.addReference("setMqttBridge", mqttBridge) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setMqttBridgeId(MQTT_BRIDGE_ID) //
						.setChargePoint(ChargePoint.CP1) //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							// Verify subscription uses CP1 topic
							assertTrue(mqttBridge.isSubscribed("openWB/internal_chargepoint/1/get/#"));
						})) //
				.deactivate();
	}

	@Test
	public void testFaultState() throws Exception {
		var mqttBridge = new DummyBridgeMqtt(MQTT_BRIDGE_ID);

		var sut = new EvcsOpenWbImpl();
		new ComponentTest(sut) //
				.addReference("setMqttBridge", mqttBridge) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setMqttBridgeId(MQTT_BRIDGE_ID) //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							// Simulate fault state (error)
							mqttBridge.simulateMessage("openWB/internal_chargepoint/0/get/fault_state", "2");
						})) //
				.deactivate();
	}
}
