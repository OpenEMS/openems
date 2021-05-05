package io.openems.edge.bridge.mqtt.api;

/**
 * The MQTT Types. ATM Only Telemetry, Command and EVENTS are available.
 * Telemetry: Send and receive Data. Depending on Key:Channel mapping
 * Command: Receive Commands at set the value to corresponding Channel.
 * Event: Sporadically implemented.
 */
public enum MqttType {
    TELEMETRY, COMMAND, EVENT
}
