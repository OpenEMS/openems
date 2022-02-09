package io.openems.edge.bridge.mqtt.api;

import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Provides the Interface for the PublishConnection.
 * It allows the PublishManager to send updates messages via mqtt over MqttConnectionPublish.
 */
public interface MqttConnectionPublish {

    /**
     * Sends the Message to the Broker. Usually called by the PublishManager.
     *
     * @param topic      Topic of the payload.
     * @param message    Payload of the message.
     * @param qos        Quality of Service of this Message.
     * @param retainFlag Should the message be retained.
     * @throws MqttException if an error occurred.
     */

    void sendMessage(String topic, String message, int qos, boolean retainFlag) throws MqttException;
}
