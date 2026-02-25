package io.openems.edge.bridge.mqtt.api;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents an MQTT message with topic, payload, and metadata.
 *
 * @param topic    the topic this message was published to or received from
 * @param payload  the message payload as byte array
 * @param qos      the Quality of Service level
 * @param retained whether this message is retained by the broker
 */
public record MqttMessage(//
		String topic, //
		byte[] payload, //
		QoS qos, //
		boolean retained) {

	public MqttMessage {
		Objects.requireNonNull(topic, "Topic must not be null");
		Objects.requireNonNull(payload, "Payload must not be null");
		Objects.requireNonNull(qos, "QoS must not be null");
	}

	/**
	 * Creates an {@link MqttMessage} with UTF-8 string payload.
	 *
	 * @param topic    the topic
	 * @param payload  the payload as string
	 * @param qos      the QoS level
	 * @param retained whether the message is retained
	 * @return the created message
	 */
	public static MqttMessage of(String topic, String payload, QoS qos, boolean retained) {
		return new MqttMessage(topic, payload.getBytes(StandardCharsets.UTF_8), qos, retained);
	}

	/**
	 * Creates an {@link MqttMessage} with default QoS (AT_LEAST_ONCE) and not
	 * retained.
	 *
	 * @param topic   the topic
	 * @param payload the payload as string
	 * @return the created message
	 */
	public static MqttMessage of(String topic, String payload) {
		return of(topic, payload, QoS.AT_LEAST_ONCE, false);
	}

	/**
	 * Gets the payload as UTF-8 string.
	 *
	 * @return the payload as string
	 */
	public String payloadAsString() {
		return new String(this.payload, StandardCharsets.UTF_8);
	}

	/**
	 * Gets the payload as UTF-8 string, or empty if payload is empty.
	 *
	 * @return optional payload string
	 */
	public Optional<String> payloadAsOptionalString() {
		if (this.payload.length == 0) {
			return Optional.empty();
		}
		return Optional.of(this.payloadAsString());
	}

}
