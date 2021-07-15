package io.openems.edge.bridge.mqtt.manager;

import io.openems.edge.bridge.mqtt.api.MqttSubscribeTask;
import io.openems.edge.bridge.mqtt.api.MqttTask;
import io.openems.edge.bridge.mqtt.api.MqttType;
import io.openems.edge.bridge.mqtt.connection.MqttConnectionSubscribeImpl;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.joda.time.DateTimeZone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by MqttBridge. Handles all SubscribeTasks.
 */
public class MqttSubscribeManager extends AbstractMqttManager {

    private final Map<MqttType, MqttConnectionSubscribeImpl> connections = new HashMap<>();


    public MqttSubscribeManager(Map<String, List<MqttTask>> subscribeTasks, String mqttBroker,
                                String mqttUsername, String mqttPassword, String mqttClientId, int keepAlive,
                                DateTimeZone timeZone) throws MqttException {

        super(mqttBroker, mqttUsername, mqttPassword, mqttClientId, keepAlive, subscribeTasks, timeZone);
        MqttType[] types = MqttType.values();
        //Create MqttConnections for each mqttType
        for (int x = 0; x < types.length; x++) {
            this.connections.put(types[x], new MqttConnectionSubscribeImpl());
            this.connections.get(types[x]).createMqttSubscribeSession(super.mqttBroker, super.mqttClientId + "_SUBSCRIBE_" + x,
                    super.mqttUsername, super.mqttPassword, super.keepAlive);
        }

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
                    ((MqttSubscribeTask) task).response(this.connections.get(task.getMqttType()).getPayload(task.getTopic()));
                    ((MqttSubscribeTask) task).convertTime(super.timeZone);
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
        MqttConnectionSubscribeImpl connection = this.connections.get(task.getMqttType());
        connection.subscribeToTopic(task.getTopic(), task.getQos(), id);
    }

    /**
     * Deactivates the Component. Called by the MqttBridge.
     * Closes the connection.
     */
    public void deactivate() {
        this.connections.forEach((key, value) -> {
            try {
                value.disconnect();
            } catch (MqttException e) {
                log.warn("Error on disconnecting: " + e.getMessage());
            }
        });
    }

    /**
     * Returns Payload from Topic.
     *
     * @param topic Topic of Payload.
     * @param type  MqttType e.g. Telemetry/Controls/events
     * @return the Payload.
     */

    public String getPayloadFromTopic(String topic, MqttType type) {
        return this.connections.get(type).getPayload(topic);
    }

    /**
     * Unsubscribes a Topic if there is no other subscriber left.
     *
     * @param task the task you wish to unsubscribe.
     * @throws MqttException if somethings wrong with the connection.
     */
    public void unsubscribeFromTopic(MqttTask task) throws MqttException {
        this.connections.get(task.getMqttType()).unsubscribeFromTopic(task.getTopic(), task.getId());
    }


    /**
     * Checks if a connection to the Mqtt Server is present.
     *
     * @return true if the connection is established
     */
    public boolean isConnected() {
        if (this.connections.values().stream().findFirst().isPresent()) {
            return this.connections.values().stream().findFirst().get().isConnected();
        }
        return false;
    }
}
