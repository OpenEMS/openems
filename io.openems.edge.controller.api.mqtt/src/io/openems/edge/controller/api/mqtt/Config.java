package io.openems.edge.controller.api.mqtt;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.channel.PersistencePriority;

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

	@AttributeDefinition(name = "Certificate", description = "The client certificate in PEM format")
	String certPem();

	@AttributeDefinition(name = "Private Key", description = "The private key in PEM format")
	String privateKeyPem();

	@AttributeDefinition(name = "Trust Store", description = "The trust store in PEM format")
	String trustStorePem();

	@AttributeDefinition(name = "Persistence Priority", description = "Send only Channels with a Persistence Priority greater-or-equals this.")
	PersistencePriority persistencePriority() default PersistencePriority.VERY_LOW;

	@AttributeDefinition(name = "Reconnection Attempt Interval", description = "Interval in seconds between reconnection attempts to the MQTT broker")
	long reconnectionAttemptInterval() default 60;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	String webconsole_configurationFactory_nameHint() default "Controller Api MQTT [{id}]";
}
