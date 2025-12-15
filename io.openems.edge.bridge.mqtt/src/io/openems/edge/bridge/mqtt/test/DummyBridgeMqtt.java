package io.openems.edge.bridge.mqtt.test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.MqttMessage;
import io.openems.edge.bridge.mqtt.api.MqttVersion;
import io.openems.edge.bridge.mqtt.api.QoS;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;

/**
 * Dummy implementation of {@link BridgeMqtt} for unit tests.
 *
 * <p>
 * This class is placed in the main source folder (not test folder) so it can be
 * used by other bundles for their unit tests.
 */
public class DummyBridgeMqtt extends AbstractDummyOpenemsComponent<DummyBridgeMqtt> implements BridgeMqtt {

	private MqttVersion mqttVersion = MqttVersion.V3_1_1;
	private boolean connected = true;

	private final Map<String, DummySubscription> subscriptions = new ConcurrentHashMap<>();
	private final Map<String, MqttMessage> publishedMessages = new ConcurrentHashMap<>();

	public DummyBridgeMqtt(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				BridgeMqtt.ChannelId.values() //
		);
	}

	@Override
	protected DummyBridgeMqtt self() {
		return this;
	}

	/**
	 * Sets the MQTT version for this dummy.
	 *
	 * @param version the version
	 * @return this
	 */
	public DummyBridgeMqtt withMqttVersion(MqttVersion version) {
		this.mqttVersion = version;
		return this;
	}

	/**
	 * Sets the connection state for this dummy.
	 *
	 * @param connected true if connected
	 * @return this
	 */
	public DummyBridgeMqtt withConnected(boolean connected) {
		this.connected = connected;
		return this;
	}

	@Override
	public MqttVersion getMqttVersion() {
		return this.mqttVersion;
	}

	@Override
	public boolean isConnected() {
		return this.connected;
	}

	@Override
	public CompletableFuture<Void> publish(String topic, String payload, QoS qos, boolean retained) {
		return this.publish(topic, payload.getBytes(), qos, retained);
	}

	@Override
	public CompletableFuture<Void> publish(String topic, byte[] payload, QoS qos, boolean retained) {
		var message = new MqttMessage(topic, payload, qos, retained);
		this.publishedMessages.put(topic, message);
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public MqttSubscription subscribe(String topicFilter, QoS qos, Consumer<MqttMessage> callback) {
		var subscription = new DummySubscription(topicFilter, qos, callback);
		this.subscriptions.put(topicFilter, subscription);
		return subscription;
	}

	@Override
	public CompletableFuture<Void> unsubscribe(MqttSubscription subscription) {
		this.subscriptions.remove(subscription.topicFilter());
		return CompletableFuture.completedFuture(null);
	}

	/**
	 * Simulates receiving a message on a topic.
	 *
	 * @param topic   the topic
	 * @param payload the payload
	 */
	public void simulateMessage(String topic, String payload) {
		var message = MqttMessage.of(topic, payload);
		this.subscriptions.forEach((filter, subscription) -> {
			if (this.topicMatchesFilter(topic, filter)) {
				subscription.callback.accept(message);
			}
		});
	}

	/**
	 * Gets the last published message to a topic.
	 *
	 * @param topic the topic
	 * @return the message or null
	 */
	public MqttMessage getPublishedMessage(String topic) {
		return this.publishedMessages.get(topic);
	}

	/**
	 * Checks if a subscription exists for a topic filter.
	 *
	 * @param topicFilter the topic filter
	 * @return true if subscribed
	 */
	public boolean isSubscribed(String topicFilter) {
		return this.subscriptions.containsKey(topicFilter);
	}

	private boolean topicMatchesFilter(String topic, String filter) {
		// Simple wildcard matching for tests
		if (filter.equals("#") || filter.equals(topic)) {
			return true;
		}
		if (filter.endsWith("#")) {
			var prefix = filter.substring(0, filter.length() - 1);
			return topic.startsWith(prefix);
		}
		return false;
	}

	private class DummySubscription implements MqttSubscription {
		private final String topicFilter;
		private final QoS qos;
		private final Consumer<MqttMessage> callback;

		DummySubscription(String topicFilter, QoS qos, Consumer<MqttMessage> callback) {
			this.topicFilter = topicFilter;
			this.qos = qos;
			this.callback = callback;
		}

		@Override
		public String topicFilter() {
			return this.topicFilter;
		}

		@Override
		public QoS qos() {
			return this.qos;
		}

		@Override
		public CompletableFuture<Void> unsubscribe() {
			return DummyBridgeMqtt.this.unsubscribe(this);
		}
	}

}
