package io.openems.edge.bridge.mqtt.handler;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.mqtt.api.MqttBridge;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttType;
import io.openems.edge.bridge.mqtt.component.MqttConfigurationComponent;
import io.openems.edge.bridge.mqtt.component.MqttConfigurationComponentImpl;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class MqttOpenemsComponentConnector extends AbstractOpenemsComponent implements MqttComponent {

    AtomicReference<MqttBridge> mqttBridge = new AtomicReference<>();
    OpenemsComponent otherComponent;

    MqttConfigurationComponent mqttConfigurationComponent;

    MqttOpenemsComponentConnector(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                                  io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, ComponentManager cpm, String mqttId)
            throws OpenemsError.OpenemsNamedException {
        super.activate(context, id, alias, enabled);
        if (cpm.getComponent(mqttId) instanceof MqttBridge) {
            this.mqttBridge.set(cpm.getComponent(mqttId));
            MqttBridge mqtt = this.mqttBridge.get();
            if (this.isEnabled() && mqtt != null && mqtt.isEnabled()) {
                this.mqttBridge.get().addMqttComponent(super.id(), this);
            }
            return true;
        }
        return false;
    }

    @Override
    public void updateJsonConfig() throws MqttException, ConfigurationException {
        this.mqttConfigurationComponent.updateJsonByChannel(new ArrayList<>(this.otherComponent.channels()), this.getConfiguration().value().get());
    }

    @Override
    public boolean isConfigured() {
        if (this.mqttConfigurationComponent != null) {
            return this.mqttConfigurationComponent.isConfigured();
        }
        return false;
    }


    void setConfiguration(MqttType mqttType, String[] subscriptionList, String[] publishList, String[] payloads,
                          boolean createdByOsgi, String mqttId, ConfigurationAdmin ca, int length,
                          String pathForJson, String payloadStyle, boolean configurationDone) throws IOException, MqttException, ConfigurationException {
        if (this.mqttBridge.get() != null && this.mqttBridge.get().isEnabled()) {
            this.mqttConfigurationComponent = new MqttConfigurationComponentImpl(subscriptionList, publishList,
                    payloads, super.id(), createdByOsgi, this.mqttBridge.get(), mqttId, mqttType);
            List<Channel<?>> channels = new ArrayList<>(otherComponent.channels());
            channels.addAll(this.channels());
            this.mqttConfigurationComponent.update(ca.getConfiguration(this.servicePid(), "?"), "channelIdList",
                    channels, length);
            if (!createdByOsgi && !pathForJson.trim().equals("")) {
                this.mqttConfigurationComponent.initJson(new ArrayList<>(this.channels()), pathForJson);

            } else if (createdByOsgi && this.mqttConfigurationComponent.hasBeenConfigured() && configurationDone) {
                this.mqttConfigurationComponent.initTasks(channels, payloadStyle);
            }

        } else {
            throw new ConfigurationException("MQTT Bridge not Defined or not Enabled!", "Check your config");
        }
    }

    @Override
    public void reactToEvent() {

    }

    @Override
    public void reactToCommand() {

    }

    void connectorDeactivate() {
        super.deactivate();
        if (this.mqttConfigurationComponent != null) {
            if (this.mqttBridge.get() != null) {
                this.mqttBridge.get().removeMqttComponent(super.id());
            }
        }
    }

    void setTelemetryComponent(String otherComponentId, ComponentManager cpm) throws OpenemsError.OpenemsNamedException {
        this.otherComponent = cpm.getComponent(otherComponentId);
    }
}
