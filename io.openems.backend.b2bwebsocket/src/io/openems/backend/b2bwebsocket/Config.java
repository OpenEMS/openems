package io.openems.backend.b2bwebsocket;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.websocket.AbstractWebsocketServer.DebugMode;

@ObjectClassDefinition(//
		name = "Backend2Backend.Websocket", //
		description = "Provides a websocket server for backend-to-backend communication.")
@interface Config {

	@AttributeDefinition(name = "Port", description = "The port of the websocket server.")
	int port() default Backend2BackendWebsocket.DEFAULT_PORT;

	@AttributeDefinition(name = "Number of Threads", description = "Pool-Size: the number of threads dedicated to handle the tasks")
	int poolSize() default 10;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	DebugMode debugMode() default DebugMode.OFF;

	String webconsole_configurationFactory_nameHint() default "Backend2Backend Websocket";

}