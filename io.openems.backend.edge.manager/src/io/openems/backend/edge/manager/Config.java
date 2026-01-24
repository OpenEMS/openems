package io.openems.backend.edge.manager;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Edge Manager", //
		description = "Configures the websocket server for OpenEMS Backend Edge Websocket")
@interface Config {

	@AttributeDefinition(name = "Port", description = "The port of the websocket server.")
	int port() default 8083;

	@AttributeDefinition(name = "Number of Threads", description = "Pool-Size: the number of threads dedicated to handle the tasks")
	int poolSize() default 10;

	String webconsole_configurationFactory_nameHint() default "Edge Manager";
}
