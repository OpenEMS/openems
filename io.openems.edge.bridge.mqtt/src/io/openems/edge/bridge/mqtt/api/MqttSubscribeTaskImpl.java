package io.openems.edge.bridge.mqtt.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.openems.edge.common.channel.Channel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;


/**
 * The Concrete Implementation of the AbstractMqttTask. The SubscribeTaskImpl handles the
 */
public class MqttSubscribeTaskImpl extends AbstractMqttTask implements MqttSubscribeTask {

    private String time;
    //converted time
    private DateTime timeDate;
    //                                               //name in Broker   // ID of channel
    //Map of ID For Broker and ChannelID --> e.g. roomTemperature: temperature.channelId.id();
    private Map<String, String> nameIdAndChannelIdMap;
    private Map<MqttCommandType, CommandWrapper> commandValueMap;

    public MqttSubscribeTaskImpl(MqttType type, MqttPriority priority, String topic, int qos, boolean retainFlag, boolean useTime,
                                 int timeToWait, Map<String, Channel<?>> channelMapForTask, String payloadForTask,
                                 PayloadStyle payloadStyle, String id, String mqttId) {
        super(topic, type, retainFlag, useTime, qos, priority, channelMapForTask, payloadForTask, timeToWait,
                payloadStyle, id, mqttId);
        if (type.equals(MqttType.TELEMETRY)) {

            this.nameIdAndChannelIdMap = new HashMap<>();
            //Important for Telemetry --> Mapping

            String[] tokens = payloadForTask.split(":");

            for (int x = 0; x < tokens.length; x += 2) {
                this.nameIdAndChannelIdMap.put(tokens[x], tokens[x + 1]);
            }
        }
        else if(type.equals(MqttType.COMMAND)) {
            commandValueMap = new HashMap<>();
            Arrays.stream(MqttCommandType.values()).forEach(consumer -> this.commandValueMap.put(consumer, new CommandWrapper("NOTDEFINED", "NOTDEFINED")));
        }
    }

    /**
     * Called by MqttSubscribeManager. Response to Payload.
     *
     * @param payload the Payload for the concrete MqttTask.
     */
    @Override
    public void response(String payload) {
        super.payloadToOrFromBroker = payload;
        standardResponse();
    }

    /**
     * Standard Response for subscription.
     * <p>Each ID from broker has a value.
     * message contains {
     * "SentOnDate": time,
     * "NameOfBrokerParam": "ID of Sensor"
     * "metrics":{
     * "NameOfBrokerParam": "Value for Param"
     * }
     * }
     * </p>
     * <p>
     * The name of broker param after metrics --> has a value, this value will be written into an Openems channel.
     * It either writes directly in the channel and sets something (e.g. subscribe to telemetry)
     * or
     * MqttType --> Each MqttComponent got a channel for corresponding MqttType and therefore each component can react to
     * entries of such channel.
     * </p>
     * <p>
     * standard Response works as follows:
     * Replace the String with  "" if it is not a Alphanumeric a decimal or a : ... the leftover string will be split
     * at the :
     * the first part is for id/Name of broker params. the second part is for value.
     * The Id/Name of broker params was already saved and therefore the index of the id matches the index of the corresponding ChannelId in the list.
     * After that the Map where the ChannelId with the corresponding Channel is stored can be called and the next Value can be set.
     * </p>
     */
    private void standardResponse() {
        String response = super.payloadToOrFromBroker;
        if (response.equals("")) {
            super.configuredPayload = response;
            return;
        }

        JsonObject responseJson = new Gson().fromJson(response, JsonObject.class);

        if (responseJson.has("time")) {
            this.time = responseJson.get("time").getAsString();
        } else if (responseJson.has("timestamp")) {
            this.time = responseJson.get("timestamp").getAsString();
        }


        switch (this.getMqttType()) {
            case TELEMETRY:
                standardTelemetryResponse(responseJson);
                break;
            case COMMAND:
                standardCommandResponse(responseJson);
                break;
            case EVENT:
                //System.out.println("Events are not supported by Subscribers yet!");
                break;
        }

    }
    private void standardCommandResponse(JsonObject tokens) {
        if (!super.getMqttType().equals(MqttType.COMMAND)) {
            return;
        }
        AtomicReference<String> commandTypeString = new AtomicReference<>("NotDefined");


        tokens.keySet().forEach(entry -> {
            if (entry.toLowerCase().contains("method")) {
                commandTypeString.set(tokens.get(entry).getAsString().toUpperCase());

            } else if (entry.toLowerCase().contains("value")) {
                this.commandValueMap.get(MqttCommandType.valueOf(commandTypeString.get())).setValue(tokens.get(entry).getAsString());
            } else if (entry.toLowerCase().contains("expires") || entry.toLowerCase().contains("expiration")) {
                if (tokens.get(entry).isJsonNull()) {
                    this.commandValueMap.get(MqttCommandType.valueOf(commandTypeString.get())).setExpiration("Infinite");
                } else {
                    this.commandValueMap.get(MqttCommandType.valueOf(commandTypeString.get())).setExpiration(tokens.get(entry).getAsString());
                }
            }
        });
    }

    /**
     * Standard Telemetry Response. Map the Data from the Broker to the OpenEMS channel.
     *
     * @param tokens response as a Json obj.
     */
    private void standardTelemetryResponse(JsonObject tokens) {
        //Events and Commands need to be handled by Component itself, only telemetry is allowed to update Channels directly.
        if (!super.getMqttType().equals(MqttType.TELEMETRY)) {
            return;
        }
        tokens.keySet().stream().filter(entry -> !entry.toUpperCase().equals("METRICS") && !entry.toUpperCase().contains("TIME") && !entry.toUpperCase().equals("ID"))
                .collect(Collectors.toList()).forEach(key -> {
            String value = tokens.get(key).getAsString();
            //Check own ChannelId Map if key is in Map and Value
            if (this.nameIdAndChannelIdMap.containsKey(key)) {
                if (!value.equals("Not Defined Yet")) {
                    String channelId = this.nameIdAndChannelIdMap.get(key);
                    Channel<?> channel = super.channels.get(channelId);
                    channel.setNextValue(value);
                    System.out.println("Update Channel: " + channelId + " with Value: " + value);
                } else {
                    System.out.println("Value not defined yet for: " + this.nameIdAndChannelIdMap.get(key));
                }
            } else {
                System.out.println("Key: " + key + " was not configured!");
            }
        });
    }


    /**
     * Converts the time. Usually Called by Manager.
     *
     * @param timeZone given by Manager-Class.
     */
    @Override
    public void convertTime(DateTimeZone timeZone) {
        if (this.time != null && !this.time.equals("")) {
            this.timeDate = DateTime.now(timeZone);
        }
    }

    @Override
    public DateTime getTime() {
        return timeDate;
    }

    /**
     * Get the Commands and their WrapperClass.
     *
     * @return The Map.
     */
    @Override
    public Map<MqttCommandType, CommandWrapper> getCommandValues() {
        return this.commandValueMap;
    }


}
