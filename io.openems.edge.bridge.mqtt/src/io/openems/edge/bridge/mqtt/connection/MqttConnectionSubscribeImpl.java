package io.openems.edge.bridge.mqtt.connection;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.openems.edge.bridge.mqtt.api.MqttConnectionSubscribe;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;

/**
 * This Class provides the MqttConnectionSubscribeImpl, that extends the AbstractMqttConnection and provides some methods for the SubscribeManager.
 * Depending on the Tasks it has n Callbacks to the MqttBroker and maps the Payload to the MqttTask(id) to a Topic and a Topic to a Payload.
 */
public class MqttConnectionSubscribeImpl extends AbstractMqttConnection implements MqttCallback, MqttConnectionSubscribe {

    //          Topics  Payload
    private final Map<String, String> subscriptions = new HashMap<>();
    //          ID      Topic
    private final Map<String, List<String>> idsAndTopics = new HashMap<>();

    private boolean callBackWasSet;

    public MqttConnectionSubscribeImpl() {
        super();
    }

    /**
     * Subscribes to topic. Usually called by Mqtt Bridge if a new MqttSubscribeTask is created.
     *
     * @param topic Topic you want to subscribe to.
     * @param qos   Quality of Service.
     * @param id    ID of the Component .e.g chp01
     * @throws MqttException if Callback is not working or Subscription fails.
     */
    @Override
    public void subscribeToTopic(String topic, int qos, String id) throws MqttException {

        super.mqttClient.subscribe(topic, qos);
        if (this.callBackWasSet == false) {
            mqttClient.setCallback(this);
            this.callBackWasSet = true;
        }
        this.subscriptions.put(topic, "");
        this.addTopicList(id, topic);
    }

    /**
     * Adds Topic with an id to List. May be useful later for Controller etc who are needing the complete Topic list of
     * a component.
     *
     * @param id    id of the component
     * @param topic topic the device is subscribing to.
     */
    private void addTopicList(String id, String topic) {
        if (this.idsAndTopics.containsKey(id)) {
            this.idsAndTopics.get(id).add(topic);
        } else {
            List<String> topicList = new ArrayList<>();
            topicList.add(topic);
            this.idsAndTopics.put(id, topicList);
        }
    }

    /**
     * Gets the Payload of a certain topic. Will be handled by subscribeTask.
     *
     * @param topic of the Payload you want to get.
     * @return the Payload.
     */

    @Override
    public String getPayload(String topic) {
        if (this.subscriptions.containsKey(topic)) {
            return this.subscriptions.get(topic);
        }

        return "";
    }


    //No Need to override --> Set To Auto Reconnect ; since there is no clean session
    // --> subscribers don't have to "resubscribe" and callback doesn't need to be set again
    @Override
    public void connectionLost(Throwable throwable) {

    }

    // REPLACE payload
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        this.subscriptions.replace(topic, new String(message.getPayload(), StandardCharsets.UTF_8));

    }


    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
    }

    /**
     * Unsubscribe from topic if it was subscribed before and no other Component is subscribed to the topic.
     * However it removes the Subscription entry in the idTopicList in the MqttConnectionSubscribeImpl
     *
     * @param topic Topic of the subscription
     * @param id    the Id of the Task.
     * @throws MqttException if a Problem with Mqtt (Broker missing etc) occurs.
     */

    @Override
    public void unsubscribeFromTopic(String topic, String id) throws MqttException {
        if (this.idsAndTopics.containsKey(id) && this.idsAndTopics.get(id).contains(topic)) {
            this.idsAndTopics.get(id).remove(topic);
            AtomicBoolean stillSubscribedTopic = new AtomicBoolean(false);
            this.idsAndTopics.forEach((taskId, containingTopic) -> {
                if (stillSubscribedTopic.get() == false) {
                    if (containingTopic.contains(topic)) {
                        stillSubscribedTopic.set(true);
                    }
                }
            });
            if (this.subscriptions.containsKey(topic) && stillSubscribedTopic.get() == false) {
                super.mqttClient.unsubscribe(topic);
            }
        }
    }

}
