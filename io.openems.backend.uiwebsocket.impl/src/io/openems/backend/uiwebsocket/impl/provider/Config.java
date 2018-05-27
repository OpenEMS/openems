package io.openems.backend.uiwebsocket.impl.provider;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "UiWebsocket", //
		description = "Configures the Websockets to OpenEMS UI")
@interface Config {
	@AttributeDefinition(name = "Port", description = "The port of the websocket server.")
	int port();

	String webconsole_configurationFactory_nameHint() default "EdgeWebsocket";
}
