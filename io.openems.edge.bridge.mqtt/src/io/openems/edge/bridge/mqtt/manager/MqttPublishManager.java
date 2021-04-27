package io.openems.edge.bridge.mqtt.manager;

import io.openems.edge.bridge.mqtt.api.MqttPublishTask;
import io.openems.edge.bridge.mqtt.api.MqttTask;
import io.openems.edge.bridge.mqtt.connection.MqttConnectionPublishImpl;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Publish Manager created by MqttBridge. Handles all Publish Tasks.
 * In One Cycle---> Handle currentToDo, calculated by AbstractMqttManager.
 */
public class MqttPublishManager extends AbstractMqttManager {
    //              QOS       MqttConnector
    private final Map<Integer, MqttConnectionPublishImpl> connections = new HashMap<>();

    public MqttPublishManager(Map<String, List<MqttTask>> publishTasks, String mqttBroker,
                              String mqttUsername, String mqttPassword, int keepAlive, String mqttClientId,
                              DateTimeZone formatter) throws MqttException {

        super(mqttBroker, mqttUsername, mqttPassword, mqttClientId, keepAlive, publishTasks,
                formatter);
        //Create new Connection Publish
        //Magic numbers bc there're only 3 QoS available
        for (int x = 0; x < 3; x++) {
            this.connections.put(x, new MqttConnectionPublishImpl());
            this.connections.get(x).createMqttPublishSession(super.mqttBroker, super.mqttClientId + "_PUBLISH_" + x,
                    super.keepAlive, super.mqttUsername, super.mqttPassword, x == 0);
            this.connections.get(x).connect();
        }
    }

    @Override
    public void forever() {
        super.foreverAbstract();
        //Handle Tasks given by Parent
        super.currentToDo.forEach(task -> {
            try {
                //Update Payload
                if (task instanceof MqttPublishTask) {
                    MqttPublishTask task1 = ((MqttPublishTask) task);
                    String now = DateTime.now(timeZone).toString("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
                    //add the Timestamp and update the Payload getting values from Channel(Each Task)
                    task1.updatePayload(now);
                }
                //Sending the message via Mqttconnection + start and stop time to check how long it does take
                //In Super class qos 0 will be "ignored" since there's no ack etc

                int qos = task.getQos();
                long time = System.currentTimeMillis();
                this.connections.get(qos).sendMessage(task.getTopic(), task.getPayload(), qos, task.getRetainFlag());
                time = System.currentTimeMillis() - time;
                //Time Calculation
                AtomicInteger counter = super.counterForQos.get(qos);
                super.timeForQos.get(qos).add(counter.get(), time);
                counter.getAndIncrement();
                counter.set(counter.get() % 30);
            } catch (MqttException e) {
                e.printStackTrace();
                //On Error add the task to future tasks to try again.
                super.toDoFuture.add(task);
            }
        });
        //If currentToDo is handled clear.
        super.currentToDo.clear();
    }


    public void deactivate() {
        super.deactivate();
        this.connections.forEach((key, value) -> {
            try {
                value.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });
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
