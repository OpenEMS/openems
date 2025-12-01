package io.openems.edge.bridge.mqtt;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.hivemq.client.mqtt.datatypes.MqttQos;

import io.openems.edge.bridge.mqtt.api.MqttMessage;
import io.openems.edge.bridge.mqtt.api.QoS;

/**
 * Common interface for MQTT connection handlers.
 *
 * <p>
 * Implementations handle protocol-specific connection logic for MQTT 3.x and
 * MQTT 5.0.
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
	 * Converts OpenEMS QoS to HiveMQ QoS.
	 *
	 * @param qos the OpenEMS QoS
	 * @return the HiveMQ QoS
	 */
	static MqttQos toHiveMqQos(QoS qos) {
		return switch (qos) {
		case AT_MOST_ONCE -> MqttQos.AT_MOST_ONCE;
		case AT_LEAST_ONCE -> MqttQos.AT_LEAST_ONCE;
		case EXACTLY_ONCE -> MqttQos.EXACTLY_ONCE;
		};
	}

	/**
	 * Converts HiveMQ QoS to OpenEMS QoS.
	 *
	 * @param qos the HiveMQ QoS
	 * @return the OpenEMS QoS
	 */
	static QoS fromHiveMqQos(MqttQos qos) {
		return switch (qos) {
		case AT_MOST_ONCE -> QoS.AT_MOST_ONCE;
		case AT_LEAST_ONCE -> QoS.AT_LEAST_ONCE;
		case EXACTLY_ONCE -> QoS.EXACTLY_ONCE;
		};
	}

}
