package io.openems.edge.bridge.mqtt.api;

import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * This provides the MqttConnectionSubscribe interface. It allows the SubscribeManager to update the Tasks and the Channel.
 * And it allows the SubscribeManager to Unsubscribe Topics (Usually called indirectly by MqttBridge)
 */
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
     * Unsubscribe from topic if it was subscribed before and no other Component is subscribed to the topic.
     * However it removes the Subscription entry in the idTopicList in the MqttConnectionSubscribeImpl
     *
     * @param topic Topic of the subscription
     * @param id    the Id of the Task.
     * @throws MqttException if a Problem with Mqtt (Broker missing etc) occurs.
     */
    void unsubscribeFromTopic(String topic, String id) throws MqttException;
}
