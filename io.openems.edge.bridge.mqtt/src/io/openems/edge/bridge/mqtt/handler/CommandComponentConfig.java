package io.openems.edge.bridge.mqtt.handler;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "MqttCommandComponent",
        description = "MqttComponent for Commands"
)
@interface  CommandComponentConfig {
    String service_pid();

    @AttributeDefinition(name = "Component Id", description = "Unique Id of the Component.")
    String id() default "MqttComponent0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";
    @AttributeDefinition(name = "OtherComponent Id", description = "The Component you want to publish.")
    String otherComponentId() default "";

    @AttributeDefinition(name = "MQTT Bridge ID", description = "Unique ID of the MqttBridge")
    String mqttBridgeId() default "MqttBridge";

    @AttributeDefinition(name = "Mqtt Id", description = "Id of this component appearing in the Broker")
    String mqttId() default "Device-0";

    @AttributeDefinition(name = "Created by OSGi", description = "Do you configure your MQTT Component by OSGi/Apache Felix or via JSON")
    boolean createdByOsgi() default true;
    @AttributeDefinition(name = "JSON File Path", description = " IF created by OSGi is false AND you want to read your config from Json File (Otherwise configure via REST later)")
    String pathForJson() default "";

    @AttributeDefinition(name = "ChannelIds", description = "This List will automatically filled with ChannelIds to configure for Pub and Sub")
    String[] channelIdList() default {};

    @AttributeDefinition(name = "Payloads. Starting with 0", description = "Type in the Payloads with ID:ChannelId:Id:ChannelId, where ID Means : Name represented in Broker and ChannelId = OpenemsChannel")
    String[] payloads() default {};

    @AttributeDefinition(name = "SubscriptionConfig", description = "This List is for configuring subscriptions, the accepted form is: "
            + "Priority!Topic!QoS!RetainFlag!TimestampUseBoolean!PayloadNo!TimeToWait")
    String[] subscriptionList() default {
            "LOW!Topic!0!true!false!0!10"
    };

    @AttributeDefinition(name = "PayloadStyle", description = "PayloadStyle you want to use (See Mqtt Information Component for more options)")
    String payloadStyle() default "STANDARD";

    boolean configurationDone() default false;

    @AttributeDefinition(name = "Missing Channel", description = "Some components add their Channel on the Fly. If you tick this, the Component checks on a regular basis if channels were added.")
    boolean componentAddsChannelOnTheFly() default false;


    boolean enabled() default true;
    String webconsole_configurationFactory_nameHint() default "MqttCommandComponent [{id}]";
}
