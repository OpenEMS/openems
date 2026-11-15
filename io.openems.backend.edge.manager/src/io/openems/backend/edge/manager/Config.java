package io.openems.backend.edge.manager;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Edge Manager", //
		description = "Configures the websocket server for OpenEMS Backend Edge Websocket")
@interface Config {

	@AttributeDefinition(name = "Port", description = "The port of the websocket server.")
	int port() default 8083;

	@AttributeDefinition(name = "Number of Threads", description = "Pool-Size: the number of threads dedicated to handle the tasks")
	int poolSize() default 10;

	@AttributeDefinition(name = "Connect secret", description = "Shared secret for Bearer token authentication between Backend Edge Application and Edge Manager. Must be at least 32 bytes (256 bit) long", type = AttributeType.PASSWORD)
	String secret();

	String webconsole_configurationFactory_nameHint() default "Edge Manager";
}
