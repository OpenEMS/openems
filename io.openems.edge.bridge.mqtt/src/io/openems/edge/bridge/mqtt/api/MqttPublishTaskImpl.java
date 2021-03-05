package io.openems.edge.bridge.mqtt.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openems.edge.common.channel.Channel;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * The concrete Implementation of the AbstractMqttTask. This component handles it's payload by getting a map of
 * the Channels it has to publish.
 * See Code for details.
 */
public class MqttPublishTaskImpl extends AbstractMqttTask implements MqttPublishTask {


    public MqttPublishTaskImpl(MqttType type, MqttPriority priority, String topic, int qos, boolean retainFlag, boolean useTime,
                               int timeToWait, Map<String, Channel<?>> channelMapForTask, String payloadForTask, PayloadStyle style, String id, String mqttId) {
        super(topic, type, retainFlag, useTime, qos, priority, channelMapForTask,
                payloadForTask, timeToWait, style, id, mqttId);

    }

    /**
     * Updates the Payload. Usually called from MqttManager.
     * ATM. Only "Standard" Payload.
     * @param now the Timestamp as a string.
     */
    @Override
    public void updatePayload(String now) {
        createStandardPayload(now);
    }

    /**
     * Creates the StandardPayload from a Config.
     *
     * @param now if Time should be added, now is added to the Payload.
     */
    private void createStandardPayload(String now) {


        JsonObject payload = new JsonObject();
        if (getAddTime()) {
            payload.addProperty("time", now);
        }
        payload.addProperty("device", super.mqttId);
        String[] configuredPayload = super.configuredPayload.split(":");
        AtomicInteger jsonCounter = new AtomicInteger(0);
        if (configuredPayload[0].equals("")) {
            return;
        }
        //the configuredPayload follows the pattern of NameInBroker:ChannelId:NameInBroker:ChannelId --> therefore key  % 2 == 0
        Arrays.stream(configuredPayload).forEachOrdered(consumer -> {
            if (jsonCounter.get() % 2 == 0) {
                String value = ""; //"Not Defined Yet";
                //get the ChannelId --> since it's ordered forEach --> get correct Channel .
                Channel<?> channel = super.channels.get(configuredPayload[jsonCounter.incrementAndGet()]);
                if (channel.value().isDefined()) {
                    JsonElement channelObj = new Gson().toJsonTree(channel.value().get());
                    payload.add(consumer, channelObj);
                } else {
                    payload.addProperty(consumer, value);
                }
            } else {
                jsonCounter.getAndIncrement();
            }
        });

        //UPDATED PAYLOAD saved.
        super.payloadToOrFromBroker = payload.toString();
    }

}

