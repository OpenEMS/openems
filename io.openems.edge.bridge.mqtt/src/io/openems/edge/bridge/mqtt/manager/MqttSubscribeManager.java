package io.openems.edge.bridge.mqtt.manager;

import io.openems.edge.bridge.mqtt.api.MqttSubscribeTask;
import io.openems.edge.bridge.mqtt.api.MqttTask;
import io.openems.edge.bridge.mqtt.connection.MqttConnectionSubscribeImpl;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.List;
import java.util.Map;

/**
 * Created by MqttBridge. Handles all SubscribeTasks.
 */
public class MqttSubscribeManager extends AbstractMqttManager {

    private final MqttConnectionSubscribeImpl connection = new MqttConnectionSubscribeImpl();

    public MqttSubscribeManager(Map<String, List<MqttTask>> subscribeTasks, String mqttBroker,
                                String mqttUsername, String mqttPassword, String mqttClientId, int keepAlive) throws MqttException {

        super(mqttBroker, mqttUsername, mqttPassword, mqttClientId, keepAlive, subscribeTasks);
        this.connection.createMqttSubscribeSession(super.mqttBroker, super.mqttClientId + "_SUBSCRIBE",
                super.mqttUsername, super.mqttPassword, super.keepAlive);

    }

    /**
     * This Method sets the current Time, and refreshes all Payloads of the current SubscribeTasks.
     */
    public void forever() {
        super.calculateCurrentTime();
        //Get all tasks and update them.
        super.allTasks.forEach((key, value) -> value.forEach(task -> {
            if (task instanceof MqttSubscribeTask) {
                //Time can be set in each config.
                if (task.isReady(super.getCurrentTime())) {
                    //Response to new message.
                    ((MqttSubscribeTask) task).response(this.connection.getPayload(task.getTopic()));
                    ((MqttSubscribeTask) task).convertTime();
                }
            }
        }));
    }


    /**
     * Can subscribe to certain topic by params of the task and id.
     *
     * @param task MqttTask usually given from bride, created by component. Type, Topic, QoS is saved here.
     * @param id   id of the Component.
     * @throws MqttException throws exception if callback fails.
     */
    public void subscribeToTopic(MqttTask task, String id) throws MqttException {
        this.connection.subscribeToTopic(task.getTopic(), task.getQos(), id);
    }

    /**
     * Deactivates the Component. Called by the MqttBridge.
     * Closes the connection.
     */
    public void deactivate() {
        try {
            this.connection.disconnect();
        } catch (MqttException e) {
            AbstractMqttManager.log.warn("Error on disconnecting: " + e.getMessage());
        }
    }

    /**
     * Returns Payload from Topic.
     *
     * @param topic Topic of Payload.
     * @return the Payload.
     */

    public String getPayloadFromTopic(String topic) {
        return this.connection.getPayload(topic);
    }

    /**
     * Unsubscribes a Topic if there is no other subscriber left.
     *
     * @param task the task you wish to unsubscribe.
     * @throws MqttException if somethings wrong with the connection.
     */
    public void unsubscribeFromTopic(MqttTask task) throws MqttException {
        this.connection.unsubscribeFromTopic(task.getTopic(), task.getId());
    }


    /**
     * Checks if a connection to the Mqtt Server is present.
     *
     * @return true if the connection is established
     */
    public boolean isConnected() {
        return this.connection.isConnected();
    }
}
