package io.openems.edge.bridge.mqtt.api;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * MQTT Bridge for OpenEMS.
 *
 * <p>
 * Provides publish/subscribe messaging capabilities with support for MQTT 3.1,
 * 3.1.1 and 5.0.
 *
 * <p>
 * Usage example:
 *
 * <pre>
 * {@code
 * @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
 * private BridgeMqtt mqttBridge;
 *
 * // Publish a message
 * this.mqttBridge.publish("openems/meter/power", "1234", QoS.AT_LEAST_ONCE);
 *
 * // Subscribe to a topic
 * this.mqttBridge.subscribe("openems/commands/#", QoS.AT_LEAST_ONCE, message -> {
 * 	System.out.println("Received: " + message.payloadAsString());
 * });
 * }
 * </pre>
 */
public interface BridgeMqtt extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Connection State.
		 *
		 * <ul>
		 * <li>Interface: BridgeMqtt
		 * <li>Type: Boolean
		 * <li>Description: True if connected to the MQTT broker
		 * </ul>
		 */
		CONNECTED(Doc.of(Level.OK)//
				.text("Connected to MQTT broker")),

		/**
		 * Connection Failed.
		 *
		 * <ul>
		 * <li>Interface: BridgeMqtt
		 * <li>Type: State
		 * <li>Level: FAULT
		 * <li>Description: Unable to connect to MQTT broker
		 * </ul>
		 */
		CONNECTION_FAILED(Doc.of(Level.FAULT)//
				.text("MQTT connection failed")),

		/**
		 * Broker Unreachable.
		 *
		 * <ul>
		 * <li>Interface: BridgeMqtt
		 * <li>Type: State
		 * <li>Level: WARNING
		 * <li>Description: MQTT broker is unreachable
		 * </ul>
		 */
		BROKER_UNREACHABLE(Doc.of(Level.WARNING)//
				.text("MQTT broker unreachable")),;

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the configured MQTT version.
	 *
	 * @return the {@link MqttVersion}
	 */
	MqttVersion getMqttVersion();

	/**
	 * Checks if the bridge is currently connected to the broker.
	 *
	 * @return true if connected
	 */
	boolean isConnected();

	/**
	 * Publishes a message to a topic.
	 *
	 * @param topic    the topic to publish to
	 * @param payload  the message payload as string
	 * @param qos      the Quality of Service level
	 * @param retained whether the message should be retained by the broker
	 * @return a {@link CompletableFuture} that completes when the message is
	 *         published
	 */
	CompletableFuture<Void> publish(String topic, String payload, QoS qos, boolean retained);

	/**
	 * Publishes a message to a topic with default settings (not retained).
	 *
	 * @param topic   the topic to publish to
	 * @param payload the message payload as string
	 * @param qos     the Quality of Service level
	 * @return a {@link CompletableFuture} that completes when the message is
	 *         published
	 */
	default CompletableFuture<Void> publish(String topic, String payload, QoS qos) {
		return this.publish(topic, payload, qos, false);
	}

	/**
	 * Publishes a message to a topic with default QoS (AT_LEAST_ONCE) and not
	 * retained.
	 *
	 * @param topic   the topic to publish to
	 * @param payload the message payload as string
	 * @return a {@link CompletableFuture} that completes when the message is
	 *         published
	 */
	default CompletableFuture<Void> publish(String topic, String payload) {
		return this.publish(topic, payload, QoS.AT_LEAST_ONCE, false);
	}

	/**
	 * Publishes a message to a topic with binary payload.
	 *
	 * @param topic    the topic to publish to
	 * @param payload  the message payload as byte array
	 * @param qos      the Quality of Service level
	 * @param retained whether the message should be retained by the broker
	 * @return a {@link CompletableFuture} that completes when the message is
	 *         published
	 */
	CompletableFuture<Void> publish(String topic, byte[] payload, QoS qos, boolean retained);

	/**
	 * Subscribes to a topic pattern.
	 *
	 * <p>
	 * Supports MQTT wildcards:
	 * <ul>
	 * <li>{@code +} - single-level wildcard (matches one topic level)
	 * <li>{@code #} - multi-level wildcard (matches any number of topic levels,
	 * must be at the end)
	 * </ul>
	 *
	 * @param topicFilter the topic filter (may include wildcards)
	 * @param qos         the maximum QoS level for received messages
	 * @param callback    the callback to invoke when a message is received
	 * @return a {@link MqttSubscription} that can be used to unsubscribe
	 */
	MqttSubscription subscribe(String topicFilter, QoS qos, Consumer<MqttMessage> callback);

	/**
	 * Subscribes to a topic pattern with default QoS (AT_LEAST_ONCE).
	 *
	 * @param topicFilter the topic filter (may include wildcards)
	 * @param callback    the callback to invoke when a message is received
	 * @return a {@link MqttSubscription} that can be used to unsubscribe
	 */
	default MqttSubscription subscribe(String topicFilter, Consumer<MqttMessage> callback) {
		return this.subscribe(topicFilter, QoS.AT_LEAST_ONCE, callback);
	}

	/**
	 * Unsubscribes from a topic.
	 *
	 * @param subscription the subscription to cancel
	 * @return a {@link CompletableFuture} that completes when unsubscribed
	 */
	CompletableFuture<Void> unsubscribe(MqttSubscription subscription);

	/**
	 * Represents an active MQTT subscription.
	 */
	interface MqttSubscription {

		/**
		 * Gets the topic filter of this subscription.
		 *
		 * @return the topic filter
		 */
		String topicFilter();

		/**
		 * Gets the QoS level of this subscription.
		 *
		 * @return the QoS level
		 */
		QoS qos();

		/**
		 * Unsubscribes from this topic.
		 *
		 * @return a {@link CompletableFuture} that completes when unsubscribed
		 */
		CompletableFuture<Void> unsubscribe();
	}

}
