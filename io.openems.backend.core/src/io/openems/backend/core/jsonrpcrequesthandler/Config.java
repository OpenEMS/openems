package io.openems.backend.core.jsonrpcrequesthandler;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core JSON-RPC Request Handler", //
		description = "The global handler for JSON-RPC Requests.")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Core JSON-RPC Request Handler";

}