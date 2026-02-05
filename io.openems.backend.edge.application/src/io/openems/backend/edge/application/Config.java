package io.openems.backend.edge.application;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Backend Edge Application", //
		description = "This serves as a proxy between OpenEMS Edge Controller.Api.Backend and OpenEMS Backend Edge-Manager")
@interface Config {

	// Related to Client

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Backend Edge Application")
	String id() default "edges0";

	@AttributeDefinition(name = "Uri", description = "The connection Uri to OpenEMS Backend Edge-Manager")
	String uri() default "ws://localhost:8083";

	@AttributeDefinition(name = "Number of Client-Threads", description = "Pool-Size: the number of threads dedicated to handle the Client tasks")
	int clientPoolSize() default 10;

	// Related to Server

	@AttributeDefinition(name = "Port", description = "The port of the websocket server for OpenEMS Edge Controller.Api.Backend")
	int port() default 8081;

	@AttributeDefinition(name = "Number of Server-Threads", description = "Pool-Size: the number of threads dedicated to handle the Server tasks")
	int serverPoolSize() default 10;

	String webconsole_configurationFactory_nameHint() default "Backend Edge Application";
}