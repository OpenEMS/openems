package io.openems.edge.bridge.mqtt.api;

/**
 * Record class for MqttData, used for connecting to a Broker, created by the
 * {@link BridgeMqtt}.
 * 
 * @param user         the user
 * @param password     the password
 * @param url          the broker url
 * @param userRequired is a user/authentication required
 * @param mqttVersion  the MqttVersion (atm only 3.1.1 is supported)
 */
public record MqttData(String user, String password, String url, boolean userRequired, int mqttVersion) {
}
