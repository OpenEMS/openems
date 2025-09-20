package io.openems.edge.controller.api.rest.readwrite;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.controller.api.rest.AbstractRestApi;

@ObjectClassDefinition(//
		name = "Controller Api REST/JSON Read-Write", //
		description = "This controller provides a read-write REST/JSON api.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlApiRest0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Port", description = "Port on which the webserver should listen.")
	int port() default 8084;

	@AttributeDefinition(name = "Connection limit", description = "Maximum number of connections")
	int connectionlimit() default 5;

	@AttributeDefinition(name = "Api-Timeout", description = "Sets the timeout in seconds for updates on Channels set by this Api.")
	int apiTimeout() default 60;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default AbstractRestApi.DEFAULT_DEBUG_MODE;

	String webconsole_configurationFactory_nameHint() default "Controller Api REST/JSON Read-Write [{id}]";

}