//package io.openems.edge.controller.fnnstb;
//
//import java.util.Random;
//
//import org.eclipse.paho.mqttv5.client.MqttClient;
//import org.eclipse.paho.mqttv5.common.MqttException;
//import org.junit.Test;
//
//import io.openems.edge.controller.fnnstb.mqtt.MqttConnectionManager;
//
//public class PublishMessageTest {
//
//	@Test
//	public void test() {
//		String topic = "AnOut_mxVal_f";
//		int numberOfMessages = 10;
//		this.publishRandomMessages(topic, numberOfMessages);
//	}
//
//	/**
//	 * Publishes a message with the given topic and payload to the MQTT broker.
//	 *
//	 * @param topic   the topic to which the message will be published.
//	 * @param payload the payload of the message.
//	 */
//	public void mqttPublish(String topic, String payload) {
//		String broker = "tcp://localhost:1883";
//		String clientId = "Publisher";
//
//		try {
//			MqttConnectionManager connectionManager = new MqttConnectionManager(broker, clientId);
//			connectionManager.connect();
//			MqttClient client = connectionManager.getClient();
//
//			int qos = 0;
//			client.publish(topic, payload.getBytes(), qos, false);
//
//			client.disconnect();
//			System.out.println("Message published: " + payload);
//		} catch (MqttException me) {
//			System.out.println("Error: " + me.getMessage());
//		}
//	}
//
//	/**
//	 * Publishes random messages with boolean payloads to the MQTT broker.
//	 *
//	 * @param topic            the topic to which the messages will be published.
//	 * @param numberOfMessages the number of random messages to publish.
//	 */
//	public void publishRandomMessages(String topic, int numberOfMessages) {
//		for (int i = 0; i < numberOfMessages; i++) {
//			String payload = "{\"signal\": " + this.getRandomBoolean() + "}";
//			this.mqttPublish(topic, payload);
//		}
//	}
//
//	private boolean getRandomBoolean() {
//		Random random = new Random();
//		return random.nextBoolean();
//	}
//
//}
