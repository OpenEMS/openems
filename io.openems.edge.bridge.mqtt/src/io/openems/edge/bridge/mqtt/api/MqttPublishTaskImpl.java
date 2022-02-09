package io.openems.edge.bridge.mqtt.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * One of the Implementations of the AbstractMqttTask. This component handles it's payload by getting a map of
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
     * The Style of the Payload can be configured and implemented.
     *
     * @param now the Timestamp as a string.
     */
    @Override
    public void updatePayload(String now) {
        switch (super.style) {

            case STANDARD:
            default:
                this.createStandardPayload(now);
                break;
        }
    }

    /**
     * <p>
     * Creates the StandardPayload from a Config.
     * Start by adding the time in UTC.
     * Then add "device" and add the mqttId.
     * After that continue to add a key:value pair, the way it was configured before.
     * foo=ChannelValue,
     * bar=AnotherChannelValue.
     * </p>
     * <p>
     * Since an Influx may have trouble to determine Booleans. Here the boolean will be converted to an integer.
     * 1 for true and 0 for false.
     * </p>
     *
     * @param now if Time should be added, now is added to the Payload.
     */
    private void createStandardPayload(String now) {


        JsonObject payload = new JsonObject();
        if (super.getAddTime()) {
            payload.addProperty("time", now);
        }
        payload.addProperty("device", super.mqttId);
        String[] configuredPayload = super.configuredPayload.split(":");
        AtomicInteger jsonCounter = new AtomicInteger(0);
        if (configuredPayload[0].equals("")) {
            return;
        }
        //The configuredPayload follows the pattern of NameInBroker:ChannelId:NameInBroker:ChannelId --> therefore key  % 2 == 0
        Arrays.stream(configuredPayload).forEachOrdered(consumer -> {
            if (jsonCounter.get() % 2 == 0) {
                String value = ""; //"Not Defined Yet";
                //Get the ChannelId --> since it's ordered forEach --> Get correct Channel .
                Channel<?> channel = super.channels.get(configuredPayload[jsonCounter.incrementAndGet()]);
                if (channel.value().isDefined()) {
                    JsonElement channelObj;
                    if (channel.getType().equals(OpenemsType.BOOLEAN)) {
                        boolean val = (Boolean) channel.value().get();
                        //influxDb can't handle boolean values on it's own
                        channelObj = new Gson().toJsonTree(val ? 1 : 0);
                    } else {
                        channelObj = new Gson().toJsonTree(channel.value().get());
                    }
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

