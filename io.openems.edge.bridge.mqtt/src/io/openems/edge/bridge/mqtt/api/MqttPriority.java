package io.openems.edge.bridge.mqtt.api;

/**
 * Priorities. Depending on the Amount of Tasks, it could be possible, not every task will be handled in time.
 * The TIME configured for each topic and the Priorities create the current tasks.
 * QoS 0 don't take much time and therefore are almost not considered.
 * Reason: QoS 1 and 2 need ACK packages etc.
 * Read: for more information http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html
 */
public enum MqttPriority {
    URGENT, HIGH, LOW
}
