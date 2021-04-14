package io.openems.edge.bridge.mqtt.api;

public interface MqttTask {

    int getQos();

    String getTopic();

    String getPayload();

    boolean getRetainFlag();

    boolean getAddTime();

    MqttPriority getPriority();

    MqttType getMqttType();

    /**
     * Called by Abstract Cycle Worker for current Tasks to handle.
     *
     * @param currentTime the currentTime, calculated each cycle by abstractManager
     * @return aboolean.
     */
    boolean isReady(long currentTime);
}
