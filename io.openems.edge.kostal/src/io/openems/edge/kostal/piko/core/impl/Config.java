package io.openems.edge.kostal.piko.core.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "KOSTAL PIKO Core", //
		description = "Implements a KOSTAL PIKO.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "kostalPiko0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "The IP address")
	String ip();

	@AttributeDefinition(name = "Unit ID", description = "The Unit ID")
	int unitID() default 0xff;

	@AttributeDefinition(name = "Port", description = "The Port")
	int port() default 81;

	String webconsole_configurationFactory_nameHint() default "KOSTAL PIKO CORE[{id}]";
}