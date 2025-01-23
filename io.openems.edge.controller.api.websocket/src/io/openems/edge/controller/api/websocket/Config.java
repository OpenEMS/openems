package io.openems.edge.controller.api.websocket;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Api Websocket", //
		description = "This controller provides an HTTP Websocket/JSON api. It is required for OpenEMS UI.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlApiWebsocket0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Port", description = "Port on which the Websocket server should listen.")
	int port() default 8085;

	@AttributeDefinition(name = "Api-Timeout", description = "Sets the timeout in seconds for updates on Channels set by this Api.")
	int apiTimeout() default 60;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	String webconsole_configurationFactory_nameHint() default "Controller Api Websocket [{id}]";
}