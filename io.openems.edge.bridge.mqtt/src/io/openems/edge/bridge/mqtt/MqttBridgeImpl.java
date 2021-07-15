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
import io.openems.edge.common.component.ComponentManager;
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
import org.osgi.service.component.annotations.Modified;
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

    @Reference
    ComponentManager cpm;

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
    void activate(ComponentContext context, Config config) throws OpenemsException, MqttException {

        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.mqttPriorities().length != MqttPriority.values().length || config.mqttTypes().length != MqttPriority.values().length) {
            this.updateConfig();
            return;
        }
        this.basicActivationOrModifiedSetup(config);
    }

    /**
     * Basic setup for modified or activation method. Sets up Time and configures the MqttSession as well as
     * setting up the manager.
     *
     * @param config the Config.
     * @throws OpenemsException thrown on error
     * @throws MqttException    thrown if publish/subscribe manager couldn't connect.
     */
    private void basicActivationOrModifiedSetup(Config config) throws OpenemsException, MqttException {
        this.timeZone = config.locale().equals("") ? DateTimeZone.UTC : DateTimeZone.forID(config.locale());
        //Important for last will.
        this.bridgePublisher = new MqttConnectionPublishImpl();
        try {
            this.createMqttSession(config);
        } catch (MqttException e) {
            this.log.warn(e.getMessage());
            throw new OpenemsException(e.getMessage());
        }

        this.publishManager = new MqttPublishManager(this.publishTasks, this.mqttBroker, this.mqttUsername,
                this.mqttPassword, config.keepAlive(), this.mqttClientId, this.timeZone);
        //ClientId --> + CLIENT_SUB_0
        this.subscribeManager = new MqttSubscribeManager(this.subscribeTasks, this.mqttBroker, this.mqttUsername,
                this.mqttPassword, this.mqttClientId, config.keepAlive(), this.timeZone);
        this.publishManager.setComponentManager(this.cpm);
        this.subscribeManager.setComponentManager(this.cpm);
        this.publishManager.setCoreCycle(config.useCoreCycleTime());
        this.subscribeManager.setCoreCycle(config.useCoreCycleTime());
    }

    /**
     * Updates Config --> MqttTypes and Priorities.
     */
    private void updateConfig() {
        Configuration c;

        try {
            c = this.ca.getConfiguration(this.servicePid(), "?");
            Dictionary<String, Object> properties = c.getProperties();
            String propertyInput = Arrays.toString(MqttType.values());

            properties.put("mqttTypes", this.propertyInput(propertyInput));
            this.setMqttTypes().setNextValue(MqttType.values());
            propertyInput = Arrays.toString(MqttPriority.values());
            properties.put("mqttPriorities", this.propertyInput(propertyInput));
            c.update(properties);

        } catch (IOException e) {
            this.log.warn("Couldn't update config, reason: " + e.getMessage());
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
            if (!(basepath.equals("") || basepath.startsWith("/"))) {
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

    @Modified
    void modified(ComponentContext context, Config config) throws MqttException, OpenemsException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.disconnectPublishAndSubscriber();
        this.basicActivationOrModifiedSetup(config);
        this.subscribeTasks.forEach((id, taskList) ->
                taskList.forEach(task -> {
                    try {
                        this.subscribeManager.subscribeToTopic(task, id);
                    } catch (MqttException e) {
                        this.log.warn("Couldn't subscribe to Topic: " + task.getTopic() + "\nReason: " + e.getMessage());
                    }
                })
        );
    }

    /**
     * Disconnects the Manager, if not null.
     *
     * @throws MqttException thrown if deactivate/disconnect fails bc of Mqtt reasons.
     */

    private void disconnectPublishAndSubscriber() throws MqttException {
        if (this.bridgePublisher != null) {
            this.bridgePublisher.disconnect();
        }
        if (this.publishManager != null) {
            this.publishManager.deactivate();
        }
        if (this.subscribeManager != null) {
            this.subscribeManager.deactivate();
        }

    }

    @Deactivate
    protected void deactivate() {
        try {
            //Disconnect every connection
            this.disconnectPublishAndSubscriber();
        } catch (MqttException e) {
            this.log.warn("Mqtt Exception on deactivation of this Bridge " + super.id() + "\nReason: " + e.getMessage());
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
                    this.log.warn("Couldn't unsubscribe from Topic: " + task.getTopic() + "reason " + e.getMessage());
                }
            });
            this.subscribeTasks.remove(id);
        }
        this.publishTasks.remove(id);
    }

    /**
     * List of all SubscribeTasks corresponding to the given device Id.
     *
     * @param id the id of the corresponding Component
     * @return the MqttTaskList.
     */

    @Override
    public List<MqttTask> getSubscribeTasks(String id) {
        if (this.subscribeTasks.containsKey(id)) {
            return this.subscribeTasks.get(id);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Gets the Publish tasks mapped to a MqttComponent.
     *
     * @param id the componentId
     * @return the List of MqttTasks
     */

    @Override
    public List<MqttTask> getPublishTasks(String id) {
        if (this.publishTasks.containsKey(id)) {
            return this.publishTasks.get(id);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Adds The MqttComponent to the Bridge. Important for Updating JSON Config and Reacting to Commands and Events.
     *
     * @param id        id of the MqttComponent usually from config of the Component
     * @param component the Component itself.
     */

    @Override
    public void addMqttComponent(String id, MqttComponent component) {
        if (this.components.containsKey(id)) {
            this.log.warn("Couldn't put the component " + id + " to the Bridge, already in Map, please use a Unique Id");
        } else {
            this.components.put(id, component);
        }
    }

    /**
     * Removes the Mqtt Component and their Tasks. Usually called on deactivation of the MqttComponent.
     *
     * @param id id of the Component you want to remove.
     */

    @Override
    public void removeMqttComponent(String id) {
        if (this.components.containsKey(id)) {
            this.components.remove(id);
            this.removeMqttTasks(id);
        }
    }


    /**
     * Checks if one of the Managers is connected to the Mqtt Server.
     *
     * @return true if the connection is established
     */

    public boolean isConnected() {
        return this.subscribeManager.isConnected() || this.publishManager.isConnected();
    }

    /**
     * Triggers the next Cycle of the manager, as well as updating the configuration of containing components.
     * Additionally components react to Commands/Mqtt Events.
     *
     * @param event the Event, usually TOPIC_CYCLE_BEFORE_PROCESS_IMAGE
     */
    @Override
    public void handleEvent(Event event) {
        if (!this.isEnabled()) {
            return;
        }
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            //handle all Tasks
            this.subscribeManager.forever();
            this.publishManager.forever();
            //Update the components Config if available
            this.components.forEach((key, value) -> {
                if (value.getConfiguration().value().isDefined() && !value.getConfiguration().value().get().equals("")) {
                    try {
                        value.updateJsonConfig();
                    } catch (MqttException | ConfigurationException e) {
                        this.log.warn("Couldn't refresh the config of component " + value.id() + " Please check your"
                                + " configuration or MqttConnection");
                    }
                }
                //React to Events and Commands
                if (value.isConfigured()) {
                    if (value.checkForMissingTasks() == false) {
                        value.reactToCommand();
                    }
                }
            });
        }
    }
}
