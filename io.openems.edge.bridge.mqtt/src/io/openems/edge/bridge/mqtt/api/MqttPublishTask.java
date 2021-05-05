package io.openems.edge.bridge.mqtt.api;

/**
 * The MqttPublish Task.
 * This Task allows Data to be published to any MQTT Broker.
 * This is created by the MqttTelemetryComponent. One Task handles 1..n OpenEMSChannel.
 * Depending on the Configuration.
 */
public interface MqttPublishTask extends MqttTask {
    /**
     * Updates the Payload with the Timestamp. Usually called by Manager.
     *
     * @param now the Timestamp as a string.
     */
    void updatePayload(String now);
}
