package io.openems.edge.bridge.mqtt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.openems.edge.bridge.mqtt.api.MqttMessage;
import io.openems.edge.bridge.mqtt.api.MqttVersion;
import io.openems.edge.bridge.mqtt.api.QoS;

public class BridgeMqttImplTest {

	@Test
	public void testDummyBridgeMqtt() throws Exception {
		var mqtt = new DummyBridgeMqtt("mqtt0") //
				.withMqttVersion(MqttVersion.V5) //
				.withConnected(true);

		assertEquals(MqttVersion.V5, mqtt.getMqttVersion());
		assertTrue(mqtt.isConnected());
	}

	@Test
	public void testPublish() throws Exception {
		var mqtt = new DummyBridgeMqtt("mqtt0");

		mqtt.publish("test/topic", "Hello MQTT", QoS.AT_LEAST_ONCE, false).get();

		var message = mqtt.getPublishedMessage("test/topic");
		assertNotNull(message);
		assertEquals("test/topic", message.topic());
		assertEquals("Hello MQTT", message.payloadAsString());
		assertEquals(QoS.AT_LEAST_ONCE, message.qos());
		assertFalse(message.retained());
	}

	@Test
	public void testSubscribe() throws Exception {
		var mqtt = new DummyBridgeMqtt("mqtt0");
		var receivedMessage = new AtomicReference<MqttMessage>();

		mqtt.subscribe("test/+/data", QoS.EXACTLY_ONCE, receivedMessage::set);

		assertTrue(mqtt.isSubscribed("test/+/data"));
	}

	@Test
	public void testSimulateMessage() throws Exception {
		var mqtt = new DummyBridgeMqtt("mqtt0");
		var receivedMessage = new AtomicReference<MqttMessage>();

		mqtt.subscribe("sensors/#", QoS.AT_LEAST_ONCE, receivedMessage::set);
		mqtt.simulateMessage("sensors/temperature", "25.5");

		assertNotNull(receivedMessage.get());
		assertEquals("sensors/temperature", receivedMessage.get().topic());
		assertEquals("25.5", receivedMessage.get().payloadAsString());
	}

	@Test
	public void testUnsubscribe() throws Exception {
		var mqtt = new DummyBridgeMqtt("mqtt0");

		var subscription = mqtt.subscribe("test/topic", msg -> {
		});
		assertTrue(mqtt.isSubscribed("test/topic"));

		subscription.unsubscribe().get();
		assertFalse(mqtt.isSubscribed("test/topic"));
	}

	@Test
	public void testMqttMessageRecord() {
		var message = MqttMessage.of("topic/test", "payload data", QoS.AT_MOST_ONCE, true);

		assertEquals("topic/test", message.topic());
		assertEquals("payload data", message.payloadAsString());
		assertEquals(QoS.AT_MOST_ONCE, message.qos());
		assertTrue(message.retained());
	}

	@Test
	public void testQoSFromValue() {
		assertEquals(QoS.AT_MOST_ONCE, QoS.fromValue(0));
		assertEquals(QoS.AT_LEAST_ONCE, QoS.fromValue(1));
		assertEquals(QoS.EXACTLY_ONCE, QoS.fromValue(2));
	}

	@Test
	public void testMqttVersionDisplayName() {
		assertEquals("3.1", MqttVersion.V3_1.getDisplayName());
		assertEquals("3.1.1", MqttVersion.V3_1_1.getDisplayName());
		assertEquals("5.0", MqttVersion.V5.getDisplayName());
	}

}
