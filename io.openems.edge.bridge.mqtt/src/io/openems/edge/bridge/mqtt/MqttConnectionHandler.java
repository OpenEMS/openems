package io.openems.edge.bridge.mqtt;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.openems.edge.bridge.mqtt.api.MqttMessage;
import io.openems.edge.bridge.mqtt.api.QoS;

/**
 * Common interface for MQTT connection handlers.
 *
 * <p>
 * Implementations handle protocol-specific connection logic for MQTT.
 */
public interface MqttConnectionHandler {

	/**
	 * Connects to the MQTT broker.
	 *
	 * @param callback callback for connection events
	 */
	void connect(ConnectionCallback callback);

	/**
	 * Disconnects from the MQTT broker.
	 */
	void disconnect();

	/**
	 * Checks if currently connected.
	 *
	 * @return true if connected
	 */
	boolean isConnected();

	/**
	 * Publishes a message.
	 *
	 * @param topic    the topic
	 * @param payload  the payload
	 * @param qos      the QoS level
	 * @param retained whether to retain
	 * @return future completing when published
	 */
	CompletableFuture<Void> publish(String topic, byte[] payload, QoS qos, boolean retained);

	/**
	 * Subscribes to a topic.
	 *
	 * @param topicFilter the topic filter
	 * @param qos         the QoS level
	 * @param callback    message callback
	 */
	void subscribe(String topicFilter, QoS qos, Consumer<MqttMessage> callback);

	/**
	 * Unsubscribes from a topic.
	 *
	 * @param topicFilter the topic filter
	 * @return future completing when unsubscribed
	 */
	CompletableFuture<Void> unsubscribe(String topicFilter);

	/**
	 * Callback interface for connection events.
	 */
	interface ConnectionCallback {

		/**
		 * Called when connection succeeds.
		 */
		void onConnected();

		/**
		 * Called when connection fails.
		 *
		 * @param error the error message
		 */
		void onConnectionFailed(String error);

		/**
		 * Called when disconnected.
		 *
		 * @param reason the disconnect reason
		 */
		void onDisconnected(String reason);
	}

	/**
	 * Converts OpenEMS QoS to Paho QoS int value.
	 *
	 * @param qos the OpenEMS QoS
	 * @return the Paho QoS int (0, 1, or 2)
	 */
	static int toPahoQos(QoS qos) {
		return switch (qos) {
		case AT_MOST_ONCE -> 0;
		case AT_LEAST_ONCE -> 1;
		case EXACTLY_ONCE -> 2;
		};
	}

	/**
	 * Converts Paho QoS int to OpenEMS QoS.
	 *
	 * @param qos the Paho QoS int (0, 1, or 2)
	 * @return the OpenEMS QoS
	 */
	static QoS fromPahoQos(int qos) {
		return switch (qos) {
		case 0 -> QoS.AT_MOST_ONCE;
		case 1 -> QoS.AT_LEAST_ONCE;
		case 2 -> QoS.EXACTLY_ONCE;
		default -> QoS.AT_MOST_ONCE;
		};
	}

}
