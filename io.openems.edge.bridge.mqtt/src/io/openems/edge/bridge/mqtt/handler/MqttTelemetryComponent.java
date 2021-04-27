package io.openems.edge.bridge.mqtt.handler;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttType;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;

import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;


@Designate(ocd = TelemetryComponentConfig.class, factory = true)
@Component(name = "MqttTelemetryComponent",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class MqttTelemetryComponent extends MqttOpenemsComponentConnector implements OpenemsComponent {


    @Reference
    ConfigurationAdmin cm;

    @Reference
    ComponentManager cpm;

    public MqttTelemetryComponent() {
        super(OpenemsComponent.ChannelId.values(), MqttComponent.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, TelemetryComponentConfig config) throws OpenemsError.OpenemsNamedException, IOException, ConfigurationException, MqttException {
        if (super.activate(context, config.id(), config.alias(), config.enabled(), cpm, config.mqttBridgeId())) {
            this.configureMqtt(config);
        } else {
            throw new ConfigurationException("Something went wrong", "Somethings wrong in Activate method");
        }
    }

    private void configureMqtt(TelemetryComponentConfig config) throws MqttException, ConfigurationException, IOException, OpenemsError.OpenemsNamedException {
        super.setTelemetryComponent(config.otherComponentId(), cpm);

        super.setConfiguration(MqttType.TELEMETRY, config.subscriptionList(), config.publishList(),
                config.payloads(), config.createdByOsgi(), config.mqttId(), cm, config.channelIdList().length,
                config.pathForJson(), config.payloadStyle(), config.configurationDone());
    }

    @Deactivate
    public void deactivate() {
        super.connectorDeactivate();
    }
}
