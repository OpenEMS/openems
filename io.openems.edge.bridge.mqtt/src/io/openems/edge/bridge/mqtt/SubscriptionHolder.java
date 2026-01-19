package io.openems.edge.bridge.mqtt;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.openems.edge.bridge.mqtt.api.BridgeMqtt.MqttSubscription;
import io.openems.edge.bridge.mqtt.api.MqttMessage;
import io.openems.edge.bridge.mqtt.api.QoS;

/**
 * Holds subscription information for re-subscription after reconnect.
 */
public class SubscriptionHolder implements MqttSubscription {

	final String topicFilter;
	final QoS qos;
	final Consumer<MqttMessage> callback;

	private final BridgeMqttImpl parent;

	/**
	 * Creates a new {@link SubscriptionHolder}.
	 *
	 * @param parent      the parent bridge
	 * @param topicFilter the topic filter
	 * @param qos         the QoS level
	 * @param callback    the message callback
	 */
	public SubscriptionHolder(BridgeMqttImpl parent, String topicFilter, QoS qos, Consumer<MqttMessage> callback) {
		this.parent = parent;
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
		return this.parent.unsubscribe(this);
	}

}
