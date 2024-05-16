package io.openems.edge.controller.fnnstb;

import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.Test;

import io.openems.edge.controller.fnnstb.mqtt.MqttConnectionManager;

public class PublishSignalPayloadTest {

	@Test
	public void test() {
		String topic = "myTopic";
		this.publishMessages(topic);
	}

	/**
	 * Publishes a message with the given topic and payload to the MQTT broker.
	 *
	 * @param topic   the topic to which the message will be published.
	 * @param payload the payload of the message.
	 */
	public void mqttPublish(String topic, String payload) {
		// String broker = "tcp://10.15.2.182:1883";
		String broker = "tcp://localhost:1883";
		String clientId = "Publisher";

		try {
			MqttConnectionManager connectionManager = new MqttConnectionManager(broker, clientId);
			connectionManager.connect();
			MqttClient client = connectionManager.getClient();

			int qos = 0;

			client.publish(topic, payload.getBytes(), qos, false);

			client.disconnect();
			System.out.println("Message published: " + payload);
		} catch (MqttException me) {
			me.printStackTrace();
			System.out.println("Error: " + me.getMessage());
		}
	}

	/**
	 * Publishes alternating true/false messages with boolean payloads to the MQTT
	 * broker, starting with false.
	 *
	 * @param topic the topic to which the messages will be published.
	 */
	public void publishMessages(String topic) {
		boolean signal = true;
		String payload = "{\"signal\": " + signal + "}";
		this.mqttPublish(topic, payload);

	}

	// {"signal" : "true"}

}
