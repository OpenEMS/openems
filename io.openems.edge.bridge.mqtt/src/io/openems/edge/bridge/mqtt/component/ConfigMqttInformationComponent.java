package io.openems.edge.bridge.mqtt.component;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@ObjectClassDefinition(
        name = "Mqtt Information Component",
        description = "Mqtt Information Component Containing: MqttTypes, MqttCommandTypes, EventTypes, Priorities and PayloadStyle.")
@interface ConfigMqttInformationComponent {

    String service_pid();

    @AttributeDefinition(name = "Mqtt Information Component - ID", description = "Id the Component.")
    String id() default "MqttInformationComponent";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "mqttTypes", description = "mqttTypes available for configuration. Following Lists will be filled automatically on Activation!")
    String[] mqttTypes() default "";

    @AttributeDefinition(name = "MqttCommandTypes", description = "MqttCommandTypes")
    String[] mqttCommandTypes() default "";

    @AttributeDefinition(name = "MqttEventTypes", description = "MqttEventTypes available")
    String[] mqttEventTypes() default "";

    //Username Pw; Type: tcp / SSL ; Keep Alive ; ClientName ; MQTT version
    @AttributeDefinition(name = "PayloadStyle", description = "PayloadStyles Available")
    String[] payloadStyle() default "";

    @AttributeDefinition(name = "mqttPriorities", description = "mqttPriorities available")
    String[] mqttPriority() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Mqtt Information Component [{id}]";
}