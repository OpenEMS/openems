package io.openems.edge.bridge.mqtt.component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openems.edge.bridge.mqtt.api.MqttBridge;
import io.openems.edge.bridge.mqtt.api.MqttPublishTask;
import io.openems.edge.bridge.mqtt.api.MqttPublishTaskImpl;
import io.openems.edge.bridge.mqtt.api.MqttSubscribeTask;
import io.openems.edge.bridge.mqtt.api.MqttSubscribeTaskImpl;
import io.openems.edge.bridge.mqtt.api.MqttType;
import io.openems.edge.bridge.mqtt.api.MqttPriority;
import io.openems.edge.bridge.mqtt.api.PayloadStyle;

import io.openems.edge.common.channel.Channel;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.joda.time.DateTime;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationException;

/**
 * This is where the most Part of the Magic happens.
 * In this component, The Publish and Subscribe Task are created and added together by config (Either OSGi or JSON)
 */
public abstract class AbstractMqttComponent {

    private final MqttBridge mqttBridge;

    private final List<String> subConfigList;
    private final List<String> pubConfigList;
    private final List<String> payloads;
    //STRING = TOPIC as ID ---- TASK
    private final Map<String, MqttPublishTask> publishTasks = new HashMap<>();
    private final Map<String, MqttSubscribeTask> subscribeTasks = new HashMap<>();
    //IF JSON IS UPTATED THOS WILL BE NEEDED
    private final Map<String, MqttPublishTask> publishTasksNew = new HashMap<>();
    private final Map<String, MqttSubscribeTask> subscribeTaskNew = new HashMap<>();

    //ChannelId ----- Channel Itself
    private final Map<String, Channel<?>> mapOfChannel = new ConcurrentHashMap<>();
    private final String id;
    private final boolean createdByOsgi;
    private boolean hasBeenConfigured;
    private String jsonConfig = "";
    private final String mqttId;
    private final MqttType mqttType;

    /**
     * Initially update Config and after that set params for initTasks.
     *  @param id            id of this Component, usually from configuredDevice and it's config.
     * @param subConfigList Subscribe ConfigList, containing the Configuration for the subscribeTasks.
     * @param pubConfigList Publish Configlist, containing the Configuration for the publishTasks.
     * @param payloads      containing all the Payloads. ConfigList got the Payload list as well.
     * @param createdByOsgi is this Component configured by OSGi or not. If not --> Read JSON File/Listen to Configuration Channel.
     * @param mqttBridge    mqttBridge of this Component.
     * @param mqttId        mqttId appearing as ID in Broker e.g. "Chp-1", leave empty for mqttID = ID of this component
     * @param mqttType      the MqttType for the Component
     */
    //Path/Qos/SpecifiedType/Payloadno     payloads     ComponentChannel
    public AbstractMqttComponent(String id, List<String> subConfigList,
                                 List<String> pubConfigList, List<String> payloads,
                                 boolean createdByOsgi, MqttBridge mqttBridge, String mqttId, MqttType mqttType) {

        this.id = id;
        this.subConfigList = subConfigList;
        this.pubConfigList = pubConfigList;
        this.payloads = payloads;
        this.createdByOsgi = createdByOsgi;
        this.mqttBridge = mqttBridge;
        this.mqttId = mqttId.equals("") ? id : mqttId;
        this.mqttType = mqttType;

    }


    /**
     * CALL THIS AFTER UPDATE IS DONE in component.
     *
     * @param channelIds usually from Parent.
     * @throws MqttException          will be thrown if a Problem occurred with the broker.
     * @throws ConfigurationException will be thrown if the configuration was wrong.
     */
    public void initTasks(List<Channel<?>> channelIds, String payloadStyle) throws MqttException, ConfigurationException {
        if (createdByOsgi) {
            createMqttTasksFromOsgi(channelIds, payloadStyle);
        }
    }


    /**
     * Creates for each config entry a pub or sub Task.
     * Add to List of MqttBridge
     * Component can get List of Tasks via Bridge and their Id
     *
     * @param channelIds usually from base Component; all channelIds.
     * @throws ConfigurationException if the Channels are Wrong
     * @throws MqttException          if a problem with Mqtt occurred
     */
    private void createMqttTasksFromOsgi(List<Channel<?>> channelIds, String payloadStyle) throws ConfigurationException, MqttException {
        if (this.pubConfigList.size() > 0 && !this.pubConfigList.get(0).equals("")) {
            createTasks(this.pubConfigList, false, channelIds, payloadStyle);
        }
        if (this.subConfigList.size() > 0 && !this.subConfigList.get(0).equals("")) {
            createTasks(this.subConfigList, true, channelIds, payloadStyle);
        }
        addTasksToBridge();

    }

    private void addTasksToBridge() throws MqttException {
        MqttException[] exMqtt = {null};
        this.subscribeTasks.forEach((key, value) -> {
            try {
                if (exMqtt[0] == null) {
                    mqttBridge.addMqttTask(this.id, value);
                    System.out.println("Added Task: " + value.getTopic());
                }
            } catch (MqttException e) {
                exMqtt[0] = e;
            }
        });
        this.publishTasks.forEach((key, value) -> {
            try {
                if (exMqtt[0] == null) {
                    mqttBridge.addMqttTask(this.id, value);
                    System.out.println("Added pub Task: " + value.getTopic());
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });
        if (exMqtt[0] != null) {
            mqttBridge.removeMqttTasks(this.id);
            throw exMqtt[0];
        }
    }

    /**
     * Create Tasks with Config given.
     *
     * @param configList usually from Parent config.
     * @param subTasks   is the current configList a sub/Pub task.
     * @param channelIds all the Channels that'll be configured
     * @param payloadStyle the Payloadstyle the component uses.
     * @throws ConfigurationException will be thrown if config is wrong/has an Error.
     */

    private void createTasks(List<String> configList, boolean subTasks, List<Channel<?>> channelIds, String payloadStyle) throws ConfigurationException {
        //
        ConfigurationException[] exConfig = {null};

        //For Each ConfigEntry (sub/pub) get the Channels and map them, create a task and add them at the end to the mqtt bridge.
        configList.forEach(entry -> {
            Map<String, Channel<?>> channelMapForTask;
            //futurePayload
            String payloadForTask;
            //split the entry; Each ConfigEntry looks like this:
            //MqttType!Priority!Topic!QoS!RetainFlag!TimeStampEnabled!PayloadNo!TimeToWait!PayloadStyle
            String[] tokens = entry.split("!");
            if (tokens.length != 7) {
                exConfig[0] = new ConfigurationException(entry, "Invalid Config");
            } else {
                //MqttType
                MqttType type = this.mqttType;

                //MqttPriority
                //Default is low for sub tasks --> no real priority
                MqttPriority priority = MqttPriority.LOW;
                if (!subTasks) {
                    priority = MqttPriority.valueOf(tokens[0].toUpperCase());
                }
                //Topic
                String topic = tokens[1];
                //Qos
                int qos = Integer.parseInt(tokens[2]);
                //RetainFlag
                boolean retainFlag = Boolean.parseBoolean(tokens[3]);
                //UseTime
                boolean useTime = Boolean.parseBoolean(tokens[4]);
                //PayloadNo
                int payloadNo = Integer.parseInt(tokens[5]);
                //TimeToWait
                int timeToWait = Integer.parseInt(tokens[6]);
                //PayloadStyle
                PayloadStyle style = PayloadStyle.valueOf(payloadStyle.toUpperCase());
                //if Error already occurred save time with this.
                if (exConfig[0] == null) {
                    try {
                        //create Map for the Tasks here, use payloadNo to identify the payload
                        channelMapForTask = configureChannelMapForTask(channelIds, payloadNo);
                        //Payload for Tasks
                        payloadForTask = this.payloads.get(payloadNo);
                        //subtasks will use payload to match their input to channels
                        if (subTasks) {
                            //ID = INTERNAL USE, MQTT ID = EXTERNAL/BROKER
                            MqttSubscribeTaskImpl task = new MqttSubscribeTaskImpl(type, priority, topic, qos, retainFlag, useTime,
                                    timeToWait, channelMapForTask, payloadForTask, style, this.id, this.mqttId);
                            this.subscribeTasks.put(topic, task);
                            //Create SubTasks
                        } else {
                            //Create PubTasks
                            //Publish tasks will use payload to map ID and the actual ChannelValue of the ChannelID
                            MqttPublishTaskImpl task = new MqttPublishTaskImpl(type, priority, topic, qos, retainFlag, useTime, timeToWait,
                                    channelMapForTask, payloadForTask, style, this.id, this.mqttId);
                            this.publishTasks.put(topic, task);
                        }

                    } catch (ConfigurationException e) {
                        exConfig[0] = e;
                    }
                }
            }
        });


        if (exConfig[0] != null) {
            throw exConfig[0];
        }

    }

    /**
     * Configure a ChannelMap for the created MqttTask.
     *
     * @param givenChannels Channel List will be Reduced each time; For better Mapping usually from Device
     * @param payloadNo     number in the playload list usually from config.
     * @return return Map of ChannelId to Channel for the Task.
     * @throws ConfigurationException if the channel is not in the map or in the channelList
     */
    private Map<String, Channel<?>> configureChannelMapForTask(List<Channel<?>> givenChannels, int payloadNo) throws ConfigurationException {
        Map<String, Channel<?>> channelMapForTask = new HashMap<>();
        String currentPayload = this.payloads.get(payloadNo);
        if (!currentPayload.contains(":")) {
            return channelMapForTask;
        }
        //PAYLOADconfig is: ID:CHANNELID:ID:CHANNELID....
        //ID == Name available in Broker
        List<String> ids = new ArrayList<>();
        //ChannelID --> Used to identify value the pub tasks get / value to put for sub task
        List<String> channelIds = new ArrayList<>();
        String[] tokens = currentPayload.split(":");

        AtomicInteger counter = new AtomicInteger(0);
        Arrays.stream(tokens).forEachOrdered(consumer -> {
            if ((counter.get() % 2) == 0) {
                ids.add(consumer);
            } else {
                channelIds.add(consumer);
            }
            counter.getAndIncrement();
        });
        if (ids.size() != channelIds.size()) {
            throw new ConfigurationException("configurePayload " + payloadNo, "Payload incorrect");
        }
        counter.set(0);
        while (counter.get() < ids.size()) {
            //ChannelID == key for Map
            String channelIdKey = channelIds.get(counter.get());
            //Check if ComponentMap already got the ID as Key
            if (this.mapOfChannel.containsKey(channelIdKey)) {
                channelMapForTask.put(channelIdKey, this.mapOfChannel.get(channelIdKey));
                //Check The Channellist and check if any Matches the channelId at the current pos. if none matches --> throw Exception --> ChannelId is wrong.
            } else if (givenChannels.stream().anyMatch(entry -> entry.channelId().id().equals(channelIds.get(counter.get())))) {
                final Channel<?>[] channelToAdd = new Channel<?>[1];
                givenChannels.stream().filter(entry -> entry.channelId().id().equals(channelIdKey)).findFirst().ifPresent(channels -> channelToAdd[0] = channels);
                //put channel into this mapChannel and to channelMap for the task. Remove from ChannelList to reduce size.
                if (channelToAdd[0] != null) {
                    this.mapOfChannel.put(channelIdKey, channelToAdd[0]);
                    channelMapForTask.put(channelIdKey, this.mapOfChannel.get(channelIdKey));
                    givenChannels.remove(channelToAdd[0]);
                }
            } else {
                throw new ConfigurationException("configurePayload", "incorrect Channel!  " + channelIds.get(counter.get()));
            }
            counter.getAndIncrement();
        }
        return channelMapForTask;
    }


    /**
     * Update method available for Components using MQTT.
     *
     * @param config        config of the Component, will be updated automatically.
     * @param configTarget  target, where to put ChannelIds. Usually something like "ChannelIds".
     * @param channelsGiven Channels of the Component, collected by this.channels, filtered by "_Property"
     * @param length        length of the configTarget entries. If Length doesn't match ChannelSize --> Update.
     */
    public void update(Configuration config, String configTarget, List<Channel<?>> channelsGiven, int length) {
        List<Channel<?>> channels =
                channelsGiven.stream().filter(entry ->
                        !entry.channelId().id().startsWith("_Property")
                ).collect(Collectors.toList());
        if (length != channels.size()) {
            this.updateConfig(config, configTarget, channels);
            hasBeenConfigured = false;

        } else {
            hasBeenConfigured = true;
        }
    }


    /**
     * Update Config and if successful you can initialize the MqttComponent.
     *
     * @param config       Configuration of the OpenemsComponent
     * @param configTarget usually from Parent-->Config.
     * @param channels     usually from Parent --> Channels.
     */

    private void updateConfig(Configuration config, String configTarget, List<Channel<?>> channels) {
        AtomicInteger counter = new AtomicInteger(0);
        String[] channelIdArray = new String[channels.size()];
        channels.forEach(channel -> channelIdArray[counter.getAndIncrement()] = channel.channelId().id());

        try {
            Dictionary<String, Object> properties = config.getProperties();
            properties.put(configTarget, propertyInput(Arrays.toString(channelIdArray)));
            config.update(properties);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Workaround for OSGi Arrays to String --> Otherwise it won't be correct.
     *
     * @param types OpenemsTypes etc
     * @return String Array which will be put to new Config
     */
    private String[] propertyInput(String types) {
        types = types.replaceAll("\\[", "");
        types = types.replaceAll("]", "");
        types = types.replace(" ", "");
        return types.split(",");
    }

    public boolean hasBeenConfigured() {
        return this.hasBeenConfigured;
    }

    /**
     * Init a Json by a String (Either from Channel Configuration OR Loaded in json File).
     *
     * @param channels   channels of the  parent.
     * @param jsonConfig The JsonConfig.
     * @throws ConfigurationException If the Config has errors.
     * @throws MqttException          If the subscription should fail.
     */
    public void initJson(List<Channel<?>> channels, String jsonConfig) throws ConfigurationException, MqttException {
        if (!this.jsonConfig.equals(jsonConfig) || !jsonConfig.equals("")) {
            this.jsonConfig = jsonConfig;
            JsonObject jsonConfigObject = new Gson().fromJson(this.jsonConfig, JsonObject.class);
            //Get payload Style and use it as an indicator how the payload should be created (ATM Only STANDARD)
            //Future: Switch case
            initStandardJson(jsonConfigObject, channels);
            //IF no errors occurred --> Remove old MqttTasks (from Bridge and locally) and add the new ones.
            this.mqttBridge.removeMqttTasks(id);
            if (!subscribeTaskNew.isEmpty()) {
                this.subscribeTasks.clear();
                this.subscribeTasks.putAll(this.subscribeTaskNew);
                this.subscribeTaskNew.clear();
            }
            if (!this.publishTasksNew.isEmpty()) {
                this.publishTasks.clear();
                this.publishTasks.putAll(this.publishTasksNew);
                this.publishTasksNew.clear();
            }
            addTasksToBridge();
        }
    }

    /**
     * Create Tasks from Standard Json Config.
     *
     * @param jsonConfigObject config from either file or sent via REST.
     * @param channels         Channels of the Component.
     * @throws ConfigurationException if there's an error with the Config.
     */
    private void initStandardJson(JsonObject jsonConfigObject, List<Channel<?>> channels) throws ConfigurationException {


        String mqttID = jsonConfigObject.get("mqttID").getAsString();
        JsonArray subscription = jsonConfigObject.getAsJsonArray("subscription");
        JsonArray publish = jsonConfigObject.getAsJsonArray("publish");
        createTasksJson(subscription, true, channels, mqttID, PayloadStyle.STANDARD, this.subscribeTasks.isEmpty());
        createTasksJson(publish, false, channels, mqttID, PayloadStyle.STANDARD, this.publishTasks.isEmpty());
        //HERE Configuration is done therefore further errors have something to do with broker and connection itself not configuration

    }

    /**
     * Creates a Task from a json. Internally called by initStandardJson Method.
     *
     * @param subOrPub either subscription or Publish tasks
     * @param isSub    indicates if the Handled payloads /Configuration are for sub or pub list
     * @param channels The Channels of Parent Component.
     * @param mqttID   internal MqttId for the Broker (e.g. CHP-1) not nec.  equal to Internal Openems ID!
     * @param style    payload style
     * @param init     check if this component was initialized or updated.
     * @throws ConfigurationException if somethings wrong with the Configuration.
     */
    private void createTasksJson(JsonArray subOrPub, boolean isSub, List<Channel<?>> channels, String mqttID, PayloadStyle style, boolean init) throws ConfigurationException {

        ConfigurationException[] exConfig = {null};

        //Each Sub Or Pub Task;
        subOrPub.forEach(consumer -> {
            if (exConfig[0] == null) {
                JsonObject subPub = consumer.getAsJsonObject();
                //Given to task
                Map<String, Channel<?>> channelMapForTask = new HashMap<>();
                //key = String and Channel = Value of key
                MqttType mqttType = MqttType.valueOf(subPub.get("mqttType").getAsString().toUpperCase());
                MqttPriority mqttPriority = MqttPriority.valueOf(subPub.get("priority").getAsString().toUpperCase());
                String topic = subPub.get("topic").getAsString();
                int qos = subPub.get("qos").getAsInt();
                boolean retain = subPub.get("retain").getAsBoolean();
                boolean useTime = subPub.get("useTime").getAsBoolean();
                int timeToWait = subPub.get("timeToWait").getAsInt();
                JsonObject payloads = new JsonObject();
                String payloadString = "";
                if (subPub.has("payload")) {

                    payloads = subPub.getAsJsonObject("payload");

                    //Payloads containing NameForBroker:ChannelId
                    payloadString = payloads.toString().replaceAll("\\{", "").replaceAll("}", "").replaceAll(",", ":").replaceAll("\"", "");
                }
                if (payloads.keySet().size() > 0) {
                    JsonObject finalPayloads = payloads;
                    payloads.keySet().forEach(key -> {
                        try {
                            //see if channelId exists
                            //Look up this Map otherwise take from list and add it to Map afterwards <-- inc. performance
                            //In the end add to channelMapForTask
                            String channelId = finalPayloads.get(key).getAsString();
                            if (this.mapOfChannel.containsKey(channelId)) {
                                channelMapForTask.put(channelId, this.mapOfChannel.get(channelId));
                            } else if (channels.stream().anyMatch(entry -> entry.channelId().id().equals(channelId))) {
                                final Channel<?>[] channelToAdd = new Channel<?>[1];
                                channels.stream().filter(entry -> entry.channelId().id().equals(channelId))
                                        .findFirst().ifPresent(channel -> channelToAdd[0] = channel);

                                if (channelToAdd[0] != null) {
                                    this.mapOfChannel.put(channelId, channelToAdd[0]);
                                    channelMapForTask.put(channelId, channelToAdd[0]);
                                    channels.remove(channelToAdd[0]);
                                } else {
                                    throw new ConfigurationException("configurePayload", "incorrect Channel! " + channelId);
                                }

                            }
                        } catch (ConfigurationException e) {
                            exConfig[0] = e;
                        }
                    });
                }
                if (isSub) {
                    MqttSubscribeTaskImpl task = new MqttSubscribeTaskImpl(mqttType, mqttPriority, topic, qos, retain, useTime, timeToWait,
                            channelMapForTask, payloadString, style, this.id, mqttID);
                    if (init) {
                        this.subscribeTasks.put(topic, task);
                    } else {
                        this.subscribeTaskNew.put(topic, task);
                    }

                } else {
                    MqttPublishTaskImpl task = new MqttPublishTaskImpl(mqttType, mqttPriority, topic, qos, retain, useTime, timeToWait,
                            channelMapForTask, payloadString, style, this.id, mqttID);
                    if (init) {
                        this.publishTasks.put(topic, task);
                    } else {
                        this.publishTasksNew.put(topic, task);
                    }
                }
            }
        });
        if (exConfig[0] != null) {
            if (init) {
                if (isSub) {
                    this.subscribeTasks.clear();
                } else {
                    this.publishTasks.clear();
                }
            }
            throw exConfig[0];
        }

    }

    /**
     * If JSON Is Read from a File, Read and give to initializer, which will also be called by Bridge if json needs to be updated.
     *
     * @param channels usually from Calling Class, All Channels.
     * @param path     Path of the JSON File
     * @throws IOException            Throw if Path is wrong.
     * @throws ConfigurationException if config from Json has an error.
     * @throws MqttException          if the subscription fails (has usually nothing to do with config but with the broker)
     */
    void initJsonFromFile(ArrayList<Channel<?>> channels, String path) throws IOException, ConfigurationException, MqttException {
        this.initJson(channels, new String(Files.readAllBytes(Paths.get(path))));
    }

    public boolean expired(MqttSubscribeTask task, int expirationTime) {
        DateTime now = new DateTime(mqttBridge.getTimeZone());
        if (task.getTime() == null) {
            return false;
        }
        DateTime expiration = task.getTime().plusSeconds(expirationTime);
        return now.isAfter(expiration);
    }

    public void setHasBeenConfigured(boolean configured){
        this.hasBeenConfigured = configured;
    }
}




