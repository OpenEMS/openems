package io.openems.backend.uiwebsocket.energydepot;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Energydepot UiWebsocketKaco", //
		description = "Configures the Websockets to Primus UI")
@interface Config {
	@AttributeDefinition(name = "Port", description = "The port of the websocket server.")
	int port();
	
	@AttributeDefinition(name = "Port", description = "The URL of the wordpress site.")
	String url() default "https://www.energydepot.de";

	String webconsole_configurationFactory_nameHint() default "EdgeWebsocket";
}
