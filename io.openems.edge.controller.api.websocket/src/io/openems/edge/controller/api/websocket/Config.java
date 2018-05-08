package io.openems.edge.controller.api.websocket;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Api Websocket", //
		description = "This controller provides an HTTP Websocket/JSON api. It is required for OpenEMS UI.")
@interface Config {
	String service_pid();

	String id() default "ctrlApiWebsocket0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Port", description = "Port on which the Websocket server should listen.")
	int port() default 8085;

	String webconsole_configurationFactory_nameHint() default "Controller Api Websocket [{id}]";
}