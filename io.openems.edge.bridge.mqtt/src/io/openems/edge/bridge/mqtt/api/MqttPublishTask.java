package io.openems.edge.bridge.mqtt.api;

public interface MqttPublishTask extends MqttTask {
    /**
     * Updates the Payload with the Timestamp. Usually called by Manager.
     *
     * @param now the Timestamp as a string.
     */
    void updatePayload(String now);
}
