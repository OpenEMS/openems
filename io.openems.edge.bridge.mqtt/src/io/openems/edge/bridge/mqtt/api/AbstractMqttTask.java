package io.openems.edge.bridge.mqtt.api;

import io.openems.edge.common.channel.Channel;

import java.util.Map;

/**
 * The Abstract MqttTask.
 * This Class contains most Data that a Task need. Specific Impl. Is found in the MqttSubscribeTaskImpl/MqttPublishTaskImpl.
 */
public abstract class AbstractMqttTask implements MqttTask {
    private final String topic;
    //Either the configured payload that will be published OR the payload from the broker this is how the subscriber gets the message
    String payloadToOrFromBroker = "";
    //COMMAND/EVENT/TELEMETRY/etc
    private final MqttType mqttType;
    private final boolean retainFlag;
    private final boolean addTime;
    private final int qos;
    private final MqttPriority mqttPriority;
    //Map of Names Channels: ChannelID is key.
    Map<String, Channel<?>> channels;
    private long timeStamp = -1;
    private final int timeToWait;
    private static final int TIME_CONVERTER_TO_SECONDS = 1000;
    //Payload either for the Pub task that needs to be changed for broker OR the handled sub task payload.
    String configuredPayload;
    PayloadStyle style;
    String id;
    String mqttId;

    AbstractMqttTask(String topic, MqttType mqttType,
                     boolean retainFlag, boolean addTime, int qos, MqttPriority priority, Map<String, Channel<?>> channels,
                     String payloadForTask, int timeToWait, PayloadStyle style, String id, String mqttId) {

        this.topic = topic;
        this.channels = channels;
        this.mqttType = mqttType;
        this.retainFlag = retainFlag;
        this.addTime = addTime;
        this.qos = qos;
        this.mqttPriority = priority;
        this.timeToWait = timeToWait;
        this.configuredPayload = payloadForTask;
        this.style = style;
        this.id = id;
        this.mqttId = mqttId;

    }

    /**
     * Get the Mqtt: Quality of Service of this Task.
     *
     * @return the QoS
     */
    @Override
    public int getQos() {
        return this.qos;
    }

    /**
     * Get the Topic of the Task.
     *
     * @return the Topic
     */
    @Override
    public String getTopic() {
        return this.topic;
    }

    /**
     * Get the Payload of the Task.
     *
     * @return the payload
     */
    @Override
    public String getPayload() {
        return this.payloadToOrFromBroker;
    }

    /**
     * Check if this task has the RetainFlag set.
     *
     * @return the boolean.
     */
    @Override
    public boolean getRetainFlag() {
        return this.retainFlag;
    }

    /**
     * Check if a Time should be added to the payload (usually only for publish tasks).
     *
     * @return the boolean.
     */
    @Override
    public boolean getAddTime() {
        return this.addTime;
    }

    /**
     * Returns the Priority of this task (Low, High, Urgent).
     *
     * @return the Priority of this task.
     */
    @Override
    public MqttPriority getPriority() {
        return this.mqttPriority;
    }

    /**
     * Get the MqttType of this task.
     *
     * @return the MqttType.
     */

    @Override
    public MqttType getMqttType() {
        return this.mqttType;
    }

    /**
     * Checks if the task is ready --> Time is up.
     *
     * @param currentTime the currentTime, calculated each cycle by abstractManager
     * @return aBoolean.
     */
    @Override
    public boolean isReady(long currentTime) {
        boolean isReady = false;
        if ((currentTime - this.timeStamp) / TIME_CONVERTER_TO_SECONDS >= this.timeToWait) {
            this.timeStamp = currentTime;
            isReady = true;
        }
        return isReady;
    }

    /**
     * Returns the Id. Used to Unsubscribe Topic.
     *
     * @return the Id of the Task.
     */

    @Override
    public String getId() {
        return this.id;
    }
}
