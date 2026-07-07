package io.openems.backend.core.jsonrpcrequesthandler;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core JSON-RPC Request Handler", //
		description = "The global handler for JSON-RPC Requests.")
@interface Config {

	@AttributeDefinition(name = "Max thread pool size for timedata queries")
	int queryThreadPoolSize() default 20;

	@AttributeDefinition(name = "Max thread pool queue size for timedata queries")
	int queryThreadPoolMaxQueueSize() default 1000;

	String webconsole_configurationFactory_nameHint() default "Core JSON-RPC Request Handler";

}