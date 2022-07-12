package io.openems.edge.bridge.mqtt.handler;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.mqtt.api.CommandWrapper;
import io.openems.edge.bridge.mqtt.api.MqttCommandType;
import io.openems.edge.bridge.mqtt.api.MqttCommands;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttSubscribeTask;
import io.openems.edge.bridge.mqtt.api.MqttType;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import org.eclipse.paho.client.mqttv3.MqttException;
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

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * The MqttCommandComponent provides a class that allows the usage of Commands, written in the MqttCommandType and MqttCommands.
 */
@Designate(ocd = CommandComponentConfig.class, factory = true)
@Component(name = "MqttCommandComponent",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE}
)
public class MqttCommandComponent extends MqttOpenemsComponentConnector implements OpenemsComponent, EventHandler {

    @Reference
    ConfigurationAdmin cm;

    @Reference
    ComponentManager cpm;
    CommandComponentConfig config;

    public MqttCommandComponent() {
        super(OpenemsComponent.ChannelId.values(), MqttComponent.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, CommandComponentConfig config) throws OpenemsError.OpenemsNamedException, IOException, ConfigurationException, MqttException {
        this.config = config;
        super.connectorDeactivate();
        if (super.activate(context, config.id(), config.alias(), config.enabled(), this.cpm, config.mqttBridgeId())) {
            this.configureMqtt(config);
        }
    }

    @Modified
    void modified(ComponentContext context, CommandComponentConfig config) throws OpenemsError.OpenemsNamedException, IOException, ConfigurationException, MqttException {
        this.config = config;
        super.modified(context, config.id(), config.alias(), config.enabled());
        super.connectorDeactivate();
        this.configureMqtt(config);
    }

    /**
     * Configures the Config for MQTT in the command Component. Here: No PublishConfig is necessary.
     *
     * @param config the Config.
     * @throws MqttException                      thrown on Mqtt Error -> subscription fail
     * @throws ConfigurationException             if something is not correctly configured in OSGi
     * @throws IOException                        if the json file given is wrong.
     * @throws OpenemsError.OpenemsNamedException if the ComponentManager couldn't find anything with corresponding id (MqttBridge)
     */
    private void configureMqtt(CommandComponentConfig config) throws MqttException, ConfigurationException, IOException, OpenemsError.OpenemsNamedException {
        if (this.isEnabled()) {
            super.setCorrespondingComponent(config.otherComponentId(), this.cpm);
            this.updateConfiguration();
        }
    }


    /**
     * React to the Command. Signal comes from the MqttBridge.
     * Get the corresponding SubscribeTasks and write CommandValues into the correct channel.
     */

    @Override
    public void reactToCommand() {
        if (super.mqttBridge.get() != null && super.otherComponent instanceof MqttCommands) {
            super.mqttBridge.get().getSubscribeTasks(super.id()).stream().filter(entry -> entry.getMqttType().equals(MqttType.COMMAND)).collect(Collectors.toList()).forEach(entry -> {
                if (entry instanceof MqttSubscribeTask) {
                    MqttSubscribeTask task = (MqttSubscribeTask) entry;
                    task.getCommandValues().forEach((key, value) -> {
                        if (this.mqttConfigurationComponent.valueLegit(value.getValue())) {
                            if (!this.mqttConfigurationComponent.expired(task, value)) {
                                this.reactToComponentCommand(key, value);
                            } else {
                                this.log.info(this.id() + " Command : " + key + " expired, resetting Channel...");
                                this.resetComponentCommandValue(key);
                            }
                        } else {
                            this.resetComponentCommandValue(key);
                        }
                    });
                }
            });
        }
    }

    /**
     * Resets the value of the command, if the command is expired.
     *
     * @param key the Command.
     */
    private void resetComponentCommandValue(MqttCommandType key) {
        if (super.otherComponent instanceof MqttCommands) {
            MqttCommands commandChannel = (MqttCommands) super.otherComponent;
            try {
                switch (key) {
                    case SETPOWER:
                        commandChannel.getSetPower().setNextWriteValue(null);
                        break;
                    case SETPERFORMANCE:
                        commandChannel.getSetPerformance().setNextWriteValue(null);
                        break;
                    case SETSCHEDULE:
                        commandChannel.getSetSchedule().setNextWriteValue(null);
                        break;
                    case SETTEMPERATURE:
                        commandChannel.getSetTemperature().setNextWriteValue(null);
                        break;
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                super.log.warn("Couldn't reset Channel for: " + this.id() + " Key: " + key);
            }
        }
    }

    /**
     * React to Command depending on the Component. MqttCommandType and Wrapper are given by calling "React to Command".
     *
     * @param key   MqttCommandType.
     * @param value Value of the MqttCommand.
     */
    private void reactToComponentCommand(MqttCommandType key, CommandWrapper value) {
        if (super.otherComponent instanceof MqttCommands) {
            MqttCommands commandChannel = (MqttCommands) super.otherComponent;
            try {
                switch (key) {
                    case SETPOWER:
                        commandChannel.getSetPower().setNextWriteValue(value.getValue());
                        break;
                    case SETPERFORMANCE:
                        commandChannel.getSetPerformance().setNextWriteValue(value.getValue());
                        break;
                    case SETSCHEDULE:
                        commandChannel.getSetSchedule().setNextWriteValue(value.getValue());
                        break;
                    case SETTEMPERATURE:
                        commandChannel.getSetTemperature().setNextWriteValue(value.getValue());
                        break;
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                super.log.warn("Couldn't set ChannelValue for: " + this.id() + " Key: " + key + "Value: " + value.getValue());
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        super.connectorDeactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled()) {
            if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
                super.renewReferenceAndMqttConfigurationComponent(this.cpm);
                if (this.mqttBridge.get() != null && this.mqttBridge.get().isEnabled()
                        && (!this.mqttBridge.get().containsComponent(this.id()) || this.mqttConfigurationComponent == null)) {
                    this.mqttBridge.get().addMqttComponent(this.id(), this);
                    try {
                        this.updateConfiguration();
                    } catch (IOException | MqttException | ConfigurationException e) {
                        super.log.warn("Couldn't apply config for this mqttComponent");
                    }
                } else {
                    try {
                        super.checkForMissingChannel(this.cm, this.config.channelIdList().length);
                    } catch (IOException e) {
                        super.log.warn("Couldn't update Channel for : " + this.id());
                    }
                }
            }
        }
    }

    private void updateConfiguration() throws ConfigurationException, MqttException, IOException {
        super.setConfiguration(MqttType.COMMAND, this.config.subscriptionList(), new String[0],
                this.config.payloads(), this.config.createdByOsgi(), this.config.mqttId(), this.cm, this.config.channelIdList().length,
                this.config.pathForJson(), this.config.payloadStyle(), this.config.configurationDone(), this.config.componentAddsChannelOnTheFly());
    }
}

