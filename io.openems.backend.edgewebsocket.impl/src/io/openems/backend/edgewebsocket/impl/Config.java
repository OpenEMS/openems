package io.openems.backend.edgewebsocket.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "EdgeWebsocket", //
		description = "Configures the Websockets to OpenEMS Edge")
@interface Config {
	@AttributeDefinition(name = "Port", description = "The port of the websocket server.")
	int port();

	String webconsole_configurationFactory_nameHint() default "EdgeWebsocket";
}
