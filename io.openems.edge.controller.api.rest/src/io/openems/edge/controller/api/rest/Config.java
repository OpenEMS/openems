package io.openems.edge.controller.api.rest;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Api REST/JSON", //
		description = "This controller provides a REST/JSON api.")
@interface Config {

	String service_pid();

	String id() default "ctrlApiRest0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Port", description = "Port on which the webserver should listen.")
	int port() default 8084;

	@AttributeDefinition(name = "Api-Timeout", description = "Sets the timeout in seconds for updates on Channels set by this Api.")
	int apiTimeout() default 60;

	String webconsole_configurationFactory_nameHint() default "Controller Api REST/JSON [{id}]";

}