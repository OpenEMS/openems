package io.openems.edge.bridge.mqtt.manager;

import io.openems.edge.bridge.mqtt.api.GetStandardZonedDateTimeFormatted;
import io.openems.edge.bridge.mqtt.api.MqttPublishTask;
import io.openems.edge.bridge.mqtt.api.MqttTask;
import io.openems.edge.bridge.mqtt.connection.MqttConnectionPublishImpl;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Publish Manager created by MqttBridge. Handles all Publish Tasks.
 * In One Cycle---> Handle currentToDo, calculated by AbstractMqttManager.
 */
public class MqttPublishManager extends AbstractMqttManager {
    private final Logger log = LoggerFactory.getLogger(MqttSubscribeManager.class);

    //              QOS       MqttConnector
    private final MqttConnectionPublishImpl connection = new MqttConnectionPublishImpl();

    public MqttPublishManager(Map<String, List<MqttTask>> publishTasks, String mqttBroker,
                              String mqttUsername, String mqttPassword, int keepAlive, String mqttClientId) throws MqttException {

        super(mqttBroker, mqttUsername, mqttPassword, mqttClientId, keepAlive, publishTasks);
        //Create new Connection Publish

        this.connection.createMqttPublishSession(super.mqttBroker, super.mqttClientId + "_PUBLISH",
                super.keepAlive, super.mqttUsername, super.mqttPassword, false);
        this.connection.connect();
    }

    /**
     * This method gets the current PublishTasks and publishes the Data to the MqttBroker.
     */
    public void forever() {
        super.foreverAbstract();
        //Handle Tasks given by Parent
        super.currentToDo.forEach(task -> {
            try {
                //Update Payload
                if (task instanceof MqttPublishTask) {
                    MqttPublishTask task1 = ((MqttPublishTask) task);
                    //see https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
                    //Example expected output:"2022-03-10T19:18:43.657+01:00"
                    String now = GetStandardZonedDateTimeFormatted.getStandardZonedDateTimeString();
                    //add the Timestamp and update the Payload getting values from Channel(Each Task)
                    task1.updatePayload(now);
                }
                //Sending the message via Mqttconnection + start and stop time to check how long it does take
                //In Super class qos 0 will be "ignored" since there's no ack etc
                int qos = task.getQos();
                long time = System.currentTimeMillis();
                this.connection.sendMessage(task.getTopic(), task.getPayload(), qos, task.getRetainFlag());
                time = System.currentTimeMillis() - time;
                //Time Calculation
                AtomicInteger counter = super.counterForQos.get(qos);
                super.timeForQos.get(qos).add(counter.get(), time);
                counter.getAndIncrement();
                counter.set(counter.get() % MAX_LIST_LENGTH);
            } catch (MqttException e) {
                log.warn("Error in Publishmanager: " + e.getMessage());
                //On Error add the task to future tasks to try again.
                super.toDoFuture.add(task);
            }
        });
        //If currentToDo is handled clear.
        super.currentToDo.clear();
    }

    /**
     * Called by MqttBridge.
     * This will disconnect every connection and stops the communication with the Broker.
     */
    public void deactivate() {

        try {
            this.connection.disconnect();
        } catch (MqttException e) {
            AbstractMqttManager.log.warn("Error on disconnecting: " + e.getMessage());
        }
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
