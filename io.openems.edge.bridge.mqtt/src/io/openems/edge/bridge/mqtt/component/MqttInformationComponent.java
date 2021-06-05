package io.openems.edge.bridge.mqtt.component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.bridge.mqtt.api.MqttType;
import io.openems.edge.bridge.mqtt.api.MqttCommandType;
import io.openems.edge.bridge.mqtt.api.MqttPriority;
import io.openems.edge.bridge.mqtt.api.PayloadStyle;
import io.openems.edge.bridge.mqtt.api.MqttEventType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;


/**
 * This is just an Information Component and can be optionally created with default options.
 * Gives an Overview of available Config settings
 * e.g. MqttCommandTypes, EventTypes, PayloadStyles etc.
 */
@Designate(ocd = ConfigMqttInformationComponent.class, factory = true)
@Component(name = "Component.Mqtt.Information",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)

public class MqttInformationComponent extends AbstractOpenemsComponent implements OpenemsComponent {


    @Reference
    ConfigurationAdmin ca;

    public MqttInformationComponent() {
        super(OpenemsComponent.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, ConfigMqttInformationComponent config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.mqttCommandTypes().length != MqttCommandType.values().length
                || config.mqttPriority().length != MqttPriority.values().length
                || config.mqttEventTypes().length != MqttEventType.values().length
                || config.mqttTypes().length != MqttType.values().length
                || config.payloadStyle().length != PayloadStyle.values().length
        ) {
            this.update();
        }


    }

    private void update() {
        Configuration c;


        try {
            c = this.ca.getConfiguration(this.servicePid(), "?");
            Dictionary<String, Object> properties = c.getProperties();

            properties.put("mqttTypes", this.propertyInput(Arrays.toString(MqttType.values())));
            properties.put("mqttPriorities", this.propertyInput(Arrays.toString(MqttPriority.values())));
            properties.put("mqttCommandTypes", this.propertyInput(Arrays.toString(MqttCommandType.values())));
            properties.put("mqttEventTypes", this.propertyInput(Arrays.toString(MqttEventType.values())));
            properties.put("payloadStyle", this.propertyInput(Arrays.toString(PayloadStyle.values())));
            properties.put("mqttPriority", this.propertyInput(Arrays.toString(MqttPriority.values())));

            c.update(properties);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] propertyInput(String types) {
        types = types.replaceAll("\\[", "");
        types = types.replaceAll("]", "");
        return types.split(",");
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }
}
