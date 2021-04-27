package io.openems.edge.bridge.mqtt.api;

import io.openems.edge.common.channel.Channel;

import java.util.Map;

/**
 * The Abstract MqttTask.
 * This Class contains most Data that a Task need. Specific Impl. Is found in the MqttSubscribeTaskImpl/MqttPublishTaskImpl
 */
public abstract class AbstractMqttTask implements MqttTask {
    private String topic;
    //Either the configured payload that will be published OR the payload from the broker this is how the subscriber gets the message
    String payloadToOrFromBroker = "";
    //COMMAND/EVENT/TELEMETRY/etc
    private MqttType mqttType;
    private boolean retainFlag;
    private boolean addTime;
    private int qos;
    private MqttPriority mqttPriority;
    //Map of Names Channels: ChannelID is key.
    Map<String, Channel<?>> channels;
    private long timeStamp = -1;
    private int timeToWait;
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

    @Override
    public int getQos() {
        return this.qos;
    }

    @Override
    public String getTopic() {
        return this.topic;
    }

    @Override
    public String getPayload() {
        return this.payloadToOrFromBroker;
    }

    @Override
    public boolean getRetainFlag() {
        return this.retainFlag;
    }

    @Override
    public boolean getAddTime() {
        return this.addTime;
    }

    @Override
    public MqttPriority getPriority() {
        return this.mqttPriority;
    }

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
        if ((currentTime - timeStamp) / 1000 >= timeToWait) {
            timeStamp = currentTime;
            isReady = true;
        }
        return isReady;
    }

}
