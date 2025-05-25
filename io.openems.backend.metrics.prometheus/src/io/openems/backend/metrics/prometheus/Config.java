package io.openems.backend.metrics.prometheus;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Metrics.Prometheus", //
		description = "Metrics endpoint for prometheus.")
@interface Config {

	@AttributeDefinition(name = "Port", description = "Http port")
	int port() default 9400;

	@AttributeDefinition(name = "Bearer Token", description = "Endpoint secured with a bearer token. Leave empty to disable authentication.", required = false)
	String bearerToken();

	String webconsole_configurationFactory_nameHint() default "Prometheus Client";

}