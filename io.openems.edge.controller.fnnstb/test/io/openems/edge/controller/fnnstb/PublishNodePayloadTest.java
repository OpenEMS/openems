package io.openems.edge.controller.fnnstb;

import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.Test;

import io.openems.edge.controller.fnnstb.mqtt.MqttConnectionManager;

public class PublishNodePayloadTest {

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
			System.out.println("Error: " + me.getMessage());
		}
	}

	/**
	 * Publishes alternating true/false messages with boolean payloads to the MQTT
	 * broker, starting with false.
	 *
	 * @param topic the topic to which the messages will be published.
	 */
//	public void publishMessages(String topic) {
//
//		String payloadMessage = "{\r\n" //
//				+ "            \"totw\": {\r\n" //
//				+ "                \"mag_f\": 123.456,\r\n" //
//				+ "                \"t\": 1234567890,\r\n" //
//				+ "                \"units_multiplier\": 1\r\n" //
//				+ "            },\r\n" //
//				+ "            \"hz\": {\r\n" //
//				+ "                \"mag_f\": 50.0,\r\n" //
//				+ "                \"t\": 1234567890,\r\n" //
//				+ "                \"units_multiplier\": 1\r\n" //
//				+ "            }\r\n" //
//				+ "        \r\n" //
//				+ "    }"; //
//
//		this.mqttPublish(topic, payloadMessage);
//
//	}

	public void publishMessages(String topic) {

		String payloadMessage = "{\r\n" + "    \"MaxConsumptionPower\": 456,\r\n"
				+ "    \"MaxProductionPower\": 167,\r\n" + "    \"PowerLimit\": 500,\r\n"
				+ "    \"PowerLimitSchedule\": [\r\n" + "        [\r\n" + "            1714120350666,\r\n"
				+ "            500\r\n" + "        ],\r\n" + "        [\r\n" + "            1714120367368,\r\n"
				+ "            0\r\n" + "        ],\r\n" + "        [\r\n" + "            1714379550666,\r\n"
				+ "            0\r\n" + "        ]\r\n" + "    ],\r\n" + "    \"ScaleFactor\": 0\r\n" + "}"; //

		this.mqttPublish(topic, payloadMessage);

	}

}
