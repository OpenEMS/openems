package io.openems.edge.bridge.mqtt;

import io.openems.edge.bridge.mqtt.api.LogVerbosity;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Bridge Mqtt v3.1.1 ", description = "Implements a CommunicationBridge for Mqtt v3.1.1")
@interface Config {

	@AttributeDefinition(name = "Id", description = "Unique ID of this component.")
	String id() default "mqttBridge0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Broker URL", description = "URL of the MQTT Broker.")
	String brokerUrl() default "";

	@AttributeDefinition(name = "User Required", description = "Does the Client need an authentication to be able to communicate with the Broker.")
	boolean userRequired() default true;

	@AttributeDefinition(name = "Username", description = "Username, used for the authentication with the Broker.")
	String username() default "";

	@AttributeDefinition(name = "Password", description = "Password", type = AttributeType.PASSWORD)
	String password() default "";

	@AttributeDefinition(name = "Log-Verbosity", description = "The log verbosity.")
	LogVerbosity logVerbosity() default LogVerbosity.NONE;

	String webconsole_configurationFactory_nameHint() default "MqttBridgev3.1.1 {id}";
}
