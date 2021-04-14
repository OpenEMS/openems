package io.openems.edge.bridge.mqtt.handler;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.mqtt.api.*;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;
import java.util.stream.Collectors;

@Designate(ocd = CommandComponentConfig.class, factory = true)
@Component(name = "MqttCommandComponent",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class MqttCommandComponent extends MqttOpenemsComponentConnector implements OpenemsComponent {

    @Reference
    ConfigurationAdmin cm;

    @Reference
    ComponentManager cpm;

    public MqttCommandComponent() {
        super(OpenemsComponent.ChannelId.values(), MqttComponent.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, CommandComponentConfig config) throws OpenemsError.OpenemsNamedException, IOException, ConfigurationException, MqttException {
        if (super.activate(context, config.id(), config.alias(), config.enabled(), cpm, config.mqttBridgeId())) {
            this.configureMqtt(config);
        } else {
            throw new ConfigurationException("Something went wrong", "Somethings wrong in Activate method");
        }
    }

    private void configureMqtt(CommandComponentConfig config) throws MqttException, ConfigurationException, IOException, OpenemsError.OpenemsNamedException {
        super.setTelemetryComponent(config.otherComponentId(), cpm);

        super.setConfiguration(MqttType.COMMAND, config.subscriptionList(), config.publishList(),
                config.payloads(), config.createdByOsgi(), config.mqttId(), cm, config.channelIdList().length,
                config.pathForJson(), config.payloadStyle(), config.configurationDone());
    }


    @Override
    public void reactToCommand() {
        if (super.mqttBridge.get() != null && super.otherComponent instanceof MqttCommands) {
            super.mqttBridge.get().getSubscribeTasks(super.id()).stream().filter(entry -> entry.getMqttType().equals(MqttType.COMMAND)).collect(Collectors.toList()).forEach(entry -> {
                if (entry instanceof MqttSubscribeTask) {
                    MqttSubscribeTask task = (MqttSubscribeTask) entry;
                    task.getCommandValues().forEach((key, value) -> {
                        if (this.mqttConfigurationComponent.valueLegit(value.getValue())) {
                            if (!this.mqttConfigurationComponent.expired(task, value)) {
                                reactToComponentCommand(key, value);
                            }
                        }
                    });
                }
            });
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
                e.printStackTrace();
            }
        }
    }

    @Deactivate
    public void deactivate() {
        super.connectorDeactivate();
    }
}

