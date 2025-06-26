package io.openems.edge.tesla.powerwall2.core;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Tesla Powerwall 2 Core", //
		description = "Implements the Tesla Powerwall 2 Core component.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "tesla0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "IP-Address of the Tesla Powerwall 2")
	String ipAddress() default "";

	@AttributeDefinition(name = "Port", description = "Port of the Tesla Powerwall 2 API")
	int port() default 443;

	String webconsole_configurationFactory_nameHint() default "Tesla Powerwall 2 Core [{id}]";

}
