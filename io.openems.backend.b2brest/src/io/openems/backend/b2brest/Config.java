package io.openems.backend.b2brest;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Backend2Backend.Rest", //
		description = "Provides a REST-Api server for backend-to-backend communication.")
@interface Config {

	@AttributeDefinition(name = "Port", description = "The port of the REST server.")
	int port() default Backend2BackendRest.DEFAULT_PORT;

	String webconsole_configurationFactory_nameHint() default "Backend2Backend Rest";

}