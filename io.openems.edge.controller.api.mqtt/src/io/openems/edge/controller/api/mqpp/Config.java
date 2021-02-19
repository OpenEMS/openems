package io.openems.edge.controller.api.mqpp;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Api MQTT", //
		description = "This controller connects to an MQTT broker")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlControllerApiMqtt";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Edge-ID", description = "Client-ID for authentication at MQTT broker")
	String clientId() default "edge0";

	@AttributeDefinition(name = "Username", description = "Username for authentication at MQTT broker")
	String username();

	@AttributeDefinition(name = "Password", description = "Password for authentication at MQTT broker", type = AttributeType.PASSWORD)
	String password();

	@AttributeDefinition(name = "Uri", description = "The connection Uri to MQTT broker.")
	String uri() default "tcp://localhost:1883";

	@AttributeDefinition(name = "Api-Timeout", description = "Sets the timeout in seconds for updates on Channels set by this Api.")
	int apiTimeout() default 60;

	String webconsole_configurationFactory_nameHint() default "Controller Api MQTT [{id}]";
}