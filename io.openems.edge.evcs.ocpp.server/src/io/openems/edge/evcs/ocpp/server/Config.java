package io.openems.edge.evcs.ocpp.server;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "EVCS OCPP Server", //
		description = "Implements a OCPP Server. Only one Server needed.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ocppServer0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	@AttributeDefinition(name = "IP-Address", description = "The IP address to listen on. ('0.0.0.0' for any IP)")
	String ip() default EvcsOcppServer.DEFAULT_IP;

	@AttributeDefinition(name = "Port", description = "The port of to listen on.")
	int port() default EvcsOcppServer.DEFAULT_PORT;

	String webconsole_configurationFactory_nameHint() default "EVCS OCPP Server [{id}]";
}