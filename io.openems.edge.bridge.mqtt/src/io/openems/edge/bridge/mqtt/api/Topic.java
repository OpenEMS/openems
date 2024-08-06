package io.openems.edge.bridge.mqtt.api;

import io.openems.edge.bridge.mqtt.api.payloads.Payload;
import io.openems.edge.bridge.mqtt.api.worker.MqttWorker;

import java.util.Objects;

/**
 * The Wrapper Class for a Topic. It has a Topic-Name (actual URI) -> either
 * where a Payload is published to, or subscribed from. A Quality of Service
 * (0-2). It also stores a Payload object, used by Publish/SubscribeTasks and
 * the {@link MqttWorker}.
 */
public class Topic {

	private final String topicName;
	private final int qos;

	private final Payload payload;

	// Are we subscribed to the topic URI ?
	private boolean isSubscribed;

	// Declares if we need to subscribe to this topic URI
	private boolean needsToBeSubscribed;

	// usually called by MqttComponents, creating SubscribeTasks
	public Topic(String topicName, Payload payload) {
		this(topicName, 1, payload, true);
	}

	// usually called by MqttComponents, creating PublishTasks
	public Topic(String topicName, int qos, Payload payload) {
		this(topicName, qos, payload, false);
	}

	public Topic(String topicName, int qos, Payload payload, boolean needsSubscription) {
		this.topicName = topicName;
		this.qos = qos;
		this.payload = payload;
		this.needsToBeSubscribed = needsSubscription;
		if (this.needsToBeSubscribed) {
			this.isSubscribed = false;
		}
	}

	public Payload getPayload() {
		return this.payload;
	}

	public String getTopicName() {
		return this.topicName;
	}

	public int getQos() {
		return this.qos;
	}

	/**
	 * When Comparing Topics, only the TopicName is in this context relevant.
	 * 
	 * @param o the compared Object
	 * @return a Boolean
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Topic topic = (Topic) o;
		return this.topicName.equals(topic.topicName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.topicName);
	}

	public boolean isSubscribed() {
		return this.isSubscribed;
	}

	public void setSubscribed(boolean subscribed) {
		this.isSubscribed = subscribed;
	}

	/**
	 * Attribute Getter for {@link #needsToBeSubscribed}.
	 *
	 * @return a boolean.
	 */
	public boolean needsToBeSubscribed() {
		return this.needsToBeSubscribed;
	}

	public void setNeedsToBeSubscribed(boolean needsToBeSubscribed) {
		this.needsToBeSubscribed = needsToBeSubscribed;
	}
}
