package io.openems.edge.bridge.mqtt.handler;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.mqtt.api.MqttBridge;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttPublishTask;
import io.openems.edge.bridge.mqtt.api.MqttSubscribeTask;
import io.openems.edge.bridge.mqtt.api.MqttType;
import io.openems.edge.bridge.mqtt.component.MqttConfigurationComponent;
import io.openems.edge.bridge.mqtt.component.MqttConfigurationComponentImpl;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.joda.time.DateTime;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class is the basis for MqttComponents to extend from (e.g. the Telemetry and Command Component)
 * It has basic Methods, that all extending Components will need (such as setConfiguration)
 */
public abstract class MqttOpenemsComponentConnector extends AbstractOpenemsComponent implements MqttComponent {
    protected final Logger log = LoggerFactory.getLogger(MqttOpenemsComponentConnector.class);
    private static final String CONFIGURATION_CHANNEL_IDENTIFICATION = "channelIdList";

    AtomicReference<MqttBridge> mqttBridge = new AtomicReference<>();
    OpenemsComponent otherComponent;
    String mqttBridgeId;
    DateTime initTime;
    private static final int WAIT_SECONDS = 10;
    private boolean initialized = false;

    MqttConfigurationComponent mqttConfigurationComponent;

    MqttOpenemsComponentConnector(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                                  io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, ComponentManager cpm, String mqttBridgeId)
            throws OpenemsError.OpenemsNamedException {
        super.activate(context, id, alias, enabled);
        this.mqttBridgeId = mqttBridgeId;
        if (cpm.getComponent(this.mqttBridgeId) instanceof MqttBridge) {
            this.mqttBridge.set(cpm.getComponent(this.mqttBridgeId));
            MqttBridge mqtt = this.mqttBridge.get();
            if (this.isEnabled() && mqtt != null && mqtt.isEnabled()) {
                this.mqttBridge.get().addMqttComponent(super.id(), this);
            } else {
                this.log.info("MqttBridge is not enabled or null! Component won't be added");
            }
            return true;
        }
        this.log.warn("The Configured MqttBridge Id is not an instance of the MqttBridge, please check your config!");
        return false;
    }

    protected boolean modified(ComponentContext context, String id, String alias,
                               boolean enabled, ComponentManager cpm, String mqttBridgeId) throws OpenemsError.OpenemsNamedException {
        super.modified(context, id, alias, enabled);
        if (cpm.getComponent(mqttBridgeId) instanceof MqttBridge) {
            this.mqttBridgeId = mqttBridgeId;
            this.mqttBridge.set(cpm.getComponent(mqttBridgeId));
            MqttBridge mqtt = this.mqttBridge.get();
            if (this.isEnabled() && mqtt != null && mqtt.isEnabled()) {
                this.mqttBridge.get().addMqttComponent(super.id(), this);

            } else {
                this.log.info("MqttBridge is not enabled or null! Component won't be added");
            }
            return true;
        }
        this.log.warn("The Configured MqttBridge Id is not an instance of the MqttBridge, please check your config!");
        return false;

    }

    /**
     * Updates the JSON Config. Called by MqttBridge.
     *
     * @throws MqttException          If a problem occurred with the mqtt connection.
     * @throws ConfigurationException if the configuration is wrong.
     */

    @Override
    public void updateJsonConfig() throws MqttException, ConfigurationException {
        this.mqttConfigurationComponent.updateJsonByChannel(new ArrayList<>(this.otherComponent.channels()), this.getConfiguration().value().get());
    }

    /**
     * Is Configuration done? --> either JSON Configuration done OR OSGi important for Bridge.
     *
     * @return aBoolean;
     */

    @Override
    public boolean isConfigured() {
        if (this.mqttConfigurationComponent != null) {
            return this.mqttConfigurationComponent.isConfigured();
        } else {
            return false;
        }
    }

    /**
     * This Method updates the Config with Channels from the Nature.
     * Sets the Configuration -> Forwards the Configuration of OSGi to the mqttConfigurationComponent.
     *
     * @param mqttType          the mqttType such as Telemetry/Command
     * @param subscriptionList  the subscription Configuration List, usually from OSGi config.
     * @param publishList       the publish Configuration List, usually from OSGi config.
     * @param payloads          the configured payloads (usually from OSGi config)
     * @param createdByOsgi     is this component configured by OSGi ?
     * @param mqttId            the Component that will show up in the broker. If this isn't configured -> defaults to OpenEms Id
     * @param ca                the ConfigurationAdmin, used for updating the Channel.
     * @param length            the length of the ChannelId entry -> is important to check if the Channels are updated in Config.
     * @param pathForJson       the Path for a local JSON File to configure this component.
     * @param payloadStyle      the PayloadStyle that was configured via OSGi.
     * @param configurationDone is the configuration Done -> set via OSGi -> one param to init.Tasks
     * @throws IOException            if File is not found (Not thrown if Path is either "" or is configuredByOsgi)
     * @throws MqttException          if connection fails
     * @throws ConfigurationException if MqttBridge is not available or config itself is wrong.
     */
    void setConfiguration(MqttType mqttType, String[] subscriptionList, String[] publishList, String[] payloads,
                          boolean createdByOsgi, String mqttId, ConfigurationAdmin ca, int length,
                          String pathForJson, String payloadStyle, boolean configurationDone) throws IOException, MqttException, ConfigurationException {
        if (this.mqttBridge.get() != null && this.mqttBridge.get().isEnabled()) {
            this.mqttConfigurationComponent = new MqttConfigurationComponentImpl(subscriptionList, publishList,
                    payloads, super.id(), createdByOsgi, this.mqttBridge.get(), mqttId, mqttType);
            List<Channel<?>> channels = new ArrayList<>(this.otherComponent.channels());
            channels.addAll(this.channels());
            this.mqttConfigurationComponent.update(ca.getConfiguration(this.servicePid(), "?"), CONFIGURATION_CHANNEL_IDENTIFICATION,
                    channels, length);
            if (!createdByOsgi && !pathForJson.trim().equals("")) {
                this.mqttConfigurationComponent.initJson(new ArrayList<>(this.channels()), pathForJson);

            } else if (createdByOsgi && this.mqttConfigurationComponent.hasBeenConfigured() && configurationDone) {
                this.mqttConfigurationComponent.initTasks(channels, payloadStyle);
            }

        } else {
            throw new ConfigurationException("setConfiguration in MqttOpenemsComponent " + this.id(), "MQTT Bridge not Defined or not Enabled!");
        }
    }

    /**
     * Placeholder to avoid impl. of empty methods in TelemetryComponent/CommandComponent etc.
     * Still important for MqttBridge.
     */
    @Override
    public void reactToEvent() {

    }

    /**
     * Placeholder to avoid impl. of empty methods in TelemetryComp / new Components that don't need this method.
     * Still important for MqttBridge and CommandComponent.
     */

    @Override
    public void reactToCommand() {

    }

    /**
     * Deactivates the CycleWorker and remove everything from the MqttBridge corresponding to this worker.
     */
    void connectorDeactivate() {
        super.deactivate();
            if (this.mqttBridge.get() != null) {
                this.mqttBridge.get().removeMqttComponent(super.id());
        }
    }

    /**
     * Get the Component via ComponentManager. This method gets the OpenEmsComponent you want to monitor/send information/ communicate via MQTT.
     *
     * @param otherComponentId the Id of the other OpenEmsComponent. Usually from Config.
     * @param cpm              the ComponentManager.
     * @throws OpenemsError.OpenemsNamedException thrown if the OpenEmsComponent with the given id couldn't be found.
     */
    void setCorrespondingComponent(String otherComponentId, ComponentManager cpm) throws OpenemsError.OpenemsNamedException {
        this.otherComponent = cpm.getComponent(otherComponentId);
    }

    protected MqttConfigurationComponent getMqttConfigurationComponent() {
        return this.mqttConfigurationComponent;
    }

    @Override
    public boolean checkForMissingTasks() {
        Map<String, MqttPublishTask> pubTasks = this.getMqttConfigurationComponent().getAbstractComponent().getPublishTasks();
        Map<String, MqttSubscribeTask> subTasks = this.getMqttConfigurationComponent().getAbstractComponent().getSubscribeTasks();
        boolean pubSizeEquals = this.mqttBridge.get().getPublishTasks(super.id()).size() == pubTasks.size();
        boolean subSizeEquals = this.mqttBridge.get().getSubscribeTasks(super.id()).size() == subTasks.size();

        if (!pubSizeEquals || !subSizeEquals) {
            this.mqttBridge.get().removeMqttTasks(super.id());
            pubTasks.forEach((topic, task) -> {
                try {
                    this.mqttBridge.get().addMqttTask(super.id(), task);
                } catch (MqttException e) {
                    this.log.warn("Couldn't add MqttTask due to an error in : " + super.id() + e.getMessage());
                }
            });
            subTasks.forEach((topic, task) -> {
                try {
                    this.mqttBridge.get().addMqttTask(super.id(), task);
                } catch (MqttException e) {
                    this.log.warn("Couldn't add MqttTask due to an error in : " + super.id() + e.getMessage());
                }
            });
            return true;
        }
        return false;
    }

    protected OpenemsComponent getOtherComponent() {
        return this.otherComponent;
    }

    /**
     * Checks if the mapped Component isEnabled or not.
     * If it is not enabled, it has probably an old reference -> renew Reference.
     *
     * @param cpm the ComponentManager.
     */
    protected void renewReferenceAndMqttConfigurationComponent(ComponentManager cpm) {
        if (this.initialized) {
            if (this.initTime != null && new DateTime().isAfter(this.initTime.plusSeconds(WAIT_SECONDS))) {
                this.initialized = false;
                try {
                    if (this.otherComponent != null && (this.otherComponent.isEnabled() == false || this.componentIsSame(cpm, this.otherComponent) == false)) {
                        this.otherComponent = cpm.getComponent(this.getOtherComponent().id());
                        this.mqttConfigurationComponent = null;
                    }
                    if (this.mqttBridge.get() == null || (this.mqttBridge.get().isEnabled() == false || this.componentIsSame(cpm, this.mqttBridge.get()) == false)) {
                        OpenemsComponent refreshedMqttBridge = cpm.getComponent(this.mqttBridgeId);
                        if (refreshedMqttBridge instanceof MqttBridge) {
                            this.mqttBridge.set((MqttBridge) refreshedMqttBridge);
                        }
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("OpenEMS component with id: " + this.getOtherComponent().id() + " Cannot be found!");
                }
            }
        } else {
            this.initialized = true;
            this.initTime = new DateTime();
        }
    }

    protected boolean componentIsSame(ComponentManager cpm, OpenemsComponent compareComponent) throws OpenemsError.OpenemsNamedException {
        OpenemsComponent registeredComponent = cpm.getComponent(compareComponent.id());
        return compareComponent.equals(registeredComponent);
    }
}
