package io.openems.edge.bridge.mqtt.api;

/**
 * Supported MQTT protocol versions.
 */
public enum MqttVersion {

	/**
	 * MQTT Version 3.1.
	 */
	V3_1("3.1"),

	/**
	 * MQTT Version 3.1.1 (most widely supported).
	 */
	V3_1_1("3.1.1"),

	/**
	 * MQTT Version 5.0 (latest, with enhanced features).
	 */
	V5("5.0");

	private final String displayName;

	MqttVersion(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Gets a human-readable display name for this MQTT version.
	 *
	 * @return the display name
	 */
	public String getDisplayName() {
		return this.displayName;
	}

}
