package io.openems.edge.bridge.mqtt.dummys;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.mqtt.api.MqttBridge;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttType;
import io.openems.edge.bridge.mqtt.api.CommandWrapper;
import io.openems.edge.bridge.mqtt.api.MqttCommandType;
import io.openems.edge.bridge.mqtt.api.MqttSubscribeTask;
import io.openems.edge.bridge.mqtt.component.MqttConfigurationComponent;
import io.openems.edge.bridge.mqtt.component.MqttConfigurationComponentImpl;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is just to show how to implement a concrete MqttReadyComponent
 * Attention: This is just  for Showcasing adapt your config etc.
 */
@Designate(ocd = ConcreteExampleConfig.class, factory = true)
@Component(name = "ConcreteMqttExample",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
//Note: ADD your nature here as well
public class ConcreteExampleComponent extends AbstractOpenemsComponent implements OpenemsComponent, DummyChannels {


    private final Logger log = LoggerFactory.getLogger(ConcreteExampleComponent.class);

    @Reference
    protected ConfigurationAdmin ca;

    //This is where the magic will happen
    private MqttConfigurationComponent mqttConfigurationComponent;

    public ConcreteExampleComponent() {
        super(OpenemsComponent.ChannelId.values(),
                DummyChannels.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, ConcreteExampleConfig config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
    }


    @Deactivate
    public void deactivate() {
        super.deactivate();
    }


}
