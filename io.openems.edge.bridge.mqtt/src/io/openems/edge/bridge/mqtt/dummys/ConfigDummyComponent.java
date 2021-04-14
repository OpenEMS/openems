package io.openems.edge.bridge.mqtt.dummys;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.List;

@ObjectClassDefinition(
        name = "Mqtt Dummy Component",
        description = "Mqtt Dummy to test the functionality of AbstrtactMqttComponent and Tasks.")
@interface ConfigDummyComponent {

    String service_pid();

    @AttributeDefinition(name = "Mqtt Dummy Component - ID", description = "Id of Mqtt Bridge.")
    String id() default "MqttBridge";

    @AttributeDefinition(name = "Mqtt ID Broker", description = "ID Appearing in MQTT")
    String mqttId() default "Chp";

    @AttributeDefinition(name = "Number", description = "No of this ComponentType")
    String number() default "0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "Use MQTT", description = "Should this component use Mqtt")
    boolean useMqtt() default true;

    @AttributeDefinition(name = "Created by Osgi Config", description = "If you want to configure via Apache Felix tick this, otherwise you need to configure via JSON File.")
    boolean createdByOsgiConfig() default true;

    @AttributeDefinition(name = "Created by JsonConfig", description = "If you want to configure via Json, put in path")
    String pathForJson() default "/home/felix/Arbeit/Repository/openems/io.openems.edge.bridge.mqtt/exampleConfig.json";

    @AttributeDefinition(name = "ChannelIds", description = "This List will automatically filled with ChannelIds to configure for Pub and Sub")
    String[] channelIdList() default {};


    @AttributeDefinition(name = "Payloads. Starting with 0", description = "Type in the Payloads with ID:ChannelId:Id:ChannelId, where ID Means : Name represented in Broker and ChannelId = OpenemsChannel")
    String[] payloads() default {};

    @AttributeDefinition(name = "SubscriptionConfig", description = "This List is for configuring subscriptions, the accepted form is: "
            + "MqttType!Priority!Topic!QoS!RetainFlag!TimestampUseBoolean!PayloadNo!TimetoWait")
    String[] subscriptionList() default {
            "MqttType!Priority!Topic!QoS!RetainFlagBoolean!TimestampUseBoolean!PayloadNo!TimeToWait"};

    @AttributeDefinition(name = "PublishConfig", description = "This List is for configuring publishes, accepted form is: "
            + "MqttType!Priority!Topic!QoS!RetainFlag!TimestampUseBoolean!PayloadNo!TimeToWait")
    String[] publishList() default {
            "MqttType!Priority!Topic!Qos!RetainFlagBoolean!TimestampUseBoolean!PayloadNo!TimeToWait"
    };

    @AttributeDefinition(name = "PayloadStyle", description = "PayloadStyle you want to use (See Mqtt Information Component for more options)")
    String payloadStyle() default "STANDARD";

    boolean configurationDone() default false;


    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Mqtt Dummy Component [{id}]";
}
