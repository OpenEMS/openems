package io.openems.edge.bridge.mqtt;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mqtt.api.MqttBridge;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttPriority;
import io.openems.edge.bridge.mqtt.api.MqttPublishTask;
import io.openems.edge.bridge.mqtt.api.MqttSubscribeTask;
import io.openems.edge.bridge.mqtt.api.MqttTask;
import io.openems.edge.bridge.mqtt.api.MqttType;
import io.openems.edge.bridge.mqtt.connection.MqttConnectionPublishImpl;
import io.openems.edge.bridge.mqtt.manager.MqttPublishManager;
import io.openems.edge.bridge.mqtt.manager.MqttSubscribeManager;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * The Mqtt Bridge.
 * <p> The MQTT Bridge builds up a Connection with the broker, esp. for last will settings.
 * The User configures Broker settings username and password. as well as last will settings and their timezone. (Defaults to UTC)
 * The Bridge creates 2 Manager. The Publish and subscribe manager, those Manager will create each multiple mqtt connections.
 * The Publish manager 3 ; Each for a QoS. and the SubscribeManager n connections depending on the mqttTypes (telemetry/commands/events)
 * </p>
 */


@Designate(ocd = Config.class, factory = true)
@Component(name = "Bridge.Mqtt",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE}
)
public class MqttBridgeImpl extends AbstractOpenemsComponent implements OpenemsComponent, MqttBridge, EventHandler {

    @Reference
    ConfigurationAdmin ca;

    private final Logger log = LoggerFactory.getLogger(MqttBridgeImpl.class);


    //Add to Manager
    //MAP OF ALL TASKS <-- ID and values in list
    private final Map<String, List<MqttTask>> publishTasks = new ConcurrentHashMap<>();
    private final Map<String, List<MqttTask>> subscribeTasks = new ConcurrentHashMap<>();

    //MqttComponentMap
    private final Map<String, MqttComponent> components = new ConcurrentHashMap<>();

    //Manager, handling the mqtt tasks and when to do what task
    private MqttPublishManager publishManager;
    private MqttSubscribeManager subscribeManager;
    //Configs
    private String mqttUsername;
    private String mqttPassword;
    private String mqttBroker;
    private String mqttClientId;

    //FOR LAST WILL
    private MqttConnectionPublishImpl bridgePublisher;

    //TimeZone Available for all classes
    private DateTimeZone timeZone = DateTimeZone.UTC;

    public MqttBridgeImpl() {
        super(OpenemsComponent.ChannelId.values(),
                MqttBridge.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsException, MqttException {

        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.mqttPriorities().length != MqttPriority.values().length || config.mqttTypes().length != MqttPriority.values().length) {
            updateConfig();
            return;
        }
        this.timeZone = config.locale().equals("") ? DateTimeZone.UTC : DateTimeZone.forID(config.locale());
        //Important for last will.
        this.bridgePublisher = new MqttConnectionPublishImpl();
        try {
            this.createMqttSession(config);
        } catch (MqttException e) {
            log.warn(e.getMessage());
            throw new OpenemsException(e.getMessage());
        }

        publishManager = new MqttPublishManager(publishTasks, this.mqttBroker, this.mqttUsername,
                this.mqttPassword, config.keepAlive(), this.mqttClientId, timeZone);
        //ClientId --> + CLIENT_SUB_0
        subscribeManager = new MqttSubscribeManager(subscribeTasks, this.mqttBroker, this.mqttUsername,
                this.mqttPassword, this.mqttClientId, config.keepAlive(), timeZone);

        publishManager.activate(super.id() + "_publish");
        subscribeManager.activate(super.id() + "_subscribe");
    }

    /**
     * Updates Config --> MqttTypes and Priorities.
     */
    private void updateConfig() {
        Configuration c;

        try {
            c = ca.getConfiguration(this.servicePid(), "?");
            Dictionary<String, Object> properties = c.getProperties();
            String types = Arrays.toString(MqttType.values());

            properties.put("mqttTypes", propertyInput(types));
            this.setMqttTypes().setNextValue(MqttType.values());
            types = Arrays.toString(MqttPriority.values());
            properties.put("mqttPriorities", propertyInput(types));
            c.update(properties);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Due to the fact that the inputs are arrays this needs to be done...it's weird but it's working.
     *
     * @param types either mqttTypes or Priorities
     * @return the String[] for update in OSGi
     */
    private String[] propertyInput(String types) {
        types = types.replaceAll("\\[", "");
        types = types.replaceAll("]", "");
        types = types.replaceAll(" ", "");
        return types.split(",");
    }

    /**
     * Creates the MQTT Session and connects to broker.
     *
     * @param config config of this mqttBridge
     * @throws MqttException if somethings wrong like pw wrong or user etc.
     */
    private void createMqttSession(Config config) throws MqttException {
        //Create Broker URL/IP etc
        //TCP SSL OR WSS
        if (config.brokerUrl().equals("")) {
            String basepath = config.basepath();
            if (basepath.equals("") || !basepath.startsWith("/")) {
                basepath = "/" + basepath;
            }
            String broker = config.connection().toLowerCase();
            broker += "://" + config.ipBroker() + ":" + config.portBroker() + basepath;
            this.mqttBroker = broker;
        } else {
            this.mqttBroker = config.brokerUrl();
        }
        this.mqttUsername = config.username();

        this.mqttPassword = config.password();
        //ClientID will be automatically altered by Managers depending on what they're doing
        this.mqttClientId = config.clientId();
        //BridgePublish set LastWill if configured
        this.bridgePublisher.createMqttPublishSession(this.mqttBroker, this.mqttClientId, config.keepAlive(),
                this.mqttUsername, this.mqttPassword, config.cleanSessionFlag());
        if (config.lastWillSet()) {
            this.bridgePublisher.addLastWill(config.topicLastWill(),
                    config.payloadLastWill(), config.qosLastWill(), config.timeStampEnabled(), config.retainedFlag(),
                    DateTime.now(this.timeZone).toString("yyyy-MM-dd'T'HH:mm:ss.SSSZZ"));
        }
        //External Call bc Last will can be set
        this.bridgePublisher.connect();

    }

    @Deactivate
    public void deactivate() {
        try {
            //Disconnect every connection
            this.bridgePublisher.disconnect();
            this.publishManager.deactivate();
            this.subscribeManager.deactivate();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    @Override
    public DateTimeZone getTimeZone() {
        return this.timeZone;
    }

    /**
     * Adds Mqtt Task to this Bridge. Usually called by AbstractMqttComponent.
     *
     * @param id       usually from MqttComponent / Same as component id
     * @param mqttTask usually created by MqttComponent
     * @throws MqttException if somethings wrong
     */

    @Override
    public void addMqttTask(String id, MqttTask mqttTask) throws MqttException {

        if (mqttTask instanceof MqttPublishTask) {
            if (this.publishTasks.containsKey(id)) {
                this.publishTasks.get(id).add(mqttTask);
            } else {
                List<MqttTask> task = new ArrayList<>();
                task.add(mqttTask);
                this.publishTasks.put(id, task);
            }
        }

        if (mqttTask instanceof MqttSubscribeTask) {
            if (this.subscribeTasks.containsKey(id)) {
                this.subscribeTasks.get(id).add(mqttTask);
            } else {
                List<MqttTask> task = new ArrayList<>();
                task.add(mqttTask);

                this.subscribeTasks.put(id, task);
            }
            this.subscribeManager.subscribeToTopic(mqttTask, id);
        }
    }

    /**
     * Removes the MqttTask by id. Usually Called by AbstractMqttComponent
     *
     * @param id usually from AbstractMqttComponent
     */
    @Override
    public void removeMqttTasks(String id) {
        if (this.subscribeTasks.containsKey(id)) {
            this.subscribeTasks.get(id).forEach(task -> {
                try {
                    this.subscribeManager.unsubscribeFromTopic(task);
                } catch (MqttException e) {
                    log.warn("Couldn't unsubscribe from Topic: " + task.getTopic() + "reason " + e.getMessage());
                }
            });
            this.subscribeTasks.remove(id);
        }
        this.publishTasks.remove(id);
    }

    @Override
    public List<MqttTask> getSubscribeTasks(String id) {
        return this.subscribeTasks.get(id);
    }

    /**
     * Adds The MqttComponent to the Bridge. Important for Updating JSON Config and Reacting to Commands and Events
     *
     * @param id        id of the MqttComponent usually from config of the Component
     * @param component the Component itself.
     */
    @Override
    public void addMqttComponent(String id, MqttComponent component) {
        if (!this.components.containsKey(id)) {
            this.components.put(id, component);
        }
    }

    @Override
    public void removeMqttComponent(String id) {
        if (this.components.containsKey(id)) {
            this.components.remove(id);
            this.removeMqttTasks(id);
        }
    }

    @Override
    public void handleEvent(Event event) {

        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            //handle all Tasks
            this.subscribeManager.triggerNextRun();
            this.publishManager.triggerNextRun();
            //Update the components Config if available
            this.components.forEach((key, value) -> {
                if (value.getConfiguration().value().isDefined() && !value.getConfiguration().value().get().equals("")) {
                    try {
                        value.updateJsonConfig();
                    } catch (MqttException | ConfigurationException e) {
                        log.warn("Couldn't refresh the config of component " + value.id() + " Please check your"
                                + " configuration or MqttConnection");
                    }
                }
                //React to Events and Commands
                if (value.isConfigured()) {
                    value.reactToEvent();
                    value.reactToCommand();
                }
            });
        }
    }
}
