package io.openems.edge.bridge.mqtt.api;

/**
 * MQTT Quality of Service levels.
 */
public enum QoS {

	/**
	 * At most once delivery. Messages may be lost.
	 */
	AT_MOST_ONCE(0),

	/**
	 * At least once delivery. Messages may be duplicated.
	 */
	AT_LEAST_ONCE(1),

	/**
	 * Exactly once delivery. Best reliability, highest overhead.
	 */
	EXACTLY_ONCE(2);

	private final int value;

	QoS(int value) {
		this.value = value;
	}

	/**
	 * Gets the numeric QoS value (0, 1, or 2).
	 *
	 * @return the QoS value
	 */
	public int getValue() {
		return this.value;
	}

	/**
	 * Gets a {@link QoS} from its numeric value.
	 *
	 * @param value the numeric value (0, 1, or 2)
	 * @return the corresponding {@link QoS}
	 * @throws IllegalArgumentException if the value is invalid
	 */
	public static QoS fromValue(int value) {
		return switch (value) {
		case 0 -> AT_MOST_ONCE;
		case 1 -> AT_LEAST_ONCE;
		case 2 -> EXACTLY_ONCE;
		default -> throw new IllegalArgumentException("Invalid QoS value: " + value);
		};
	}

}
