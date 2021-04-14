package io.openems.edge.bridge.mqtt.api;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.List;

public interface MqttConnectionSubscribe {

    /**
     * A Task subscribing to Topic. Adds an additional Topic to the Connection.
     *
     * @param topic Topic you want to subscribe to.
     * @param qos   Quality of Service.
     * @param id    ID of the Component .e.g chp01
     * @throws MqttException if an error occurred.
     */
    void subscribeToTopic(String topic, int qos, String id) throws MqttException;

    /**
     * Gets the Payload of a certain topic. Will be handled by subscribeTask.
     *
     * @param topic of the Payload you want to get.
     * @return the Payload.
     */
    String getPayload(String topic);

    /**
     * In case you need an overview of topics the Component by the id subscribed to.
     *
     * @param id the id of the component.
     * @return the List of Topics.
     */
    List<String> getTopic(String id);

    /**
     * Unsubscribe from topic if it was subscribed before.
     *
     * @param topic Topic of the subscription
     * @throws MqttException if a Problem with Mqtt (Broker missing etc) occurs.
     */
    void unsubscribeFromTopic(String topic) throws MqttException;
}
