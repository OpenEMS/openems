package io.openems.edge.evcs.hardybarth;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "EVCS Hardy Barth", //
		description = "Implements the Hardy Barth - Salia electric vehicle charging station.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcs0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the charging station.", required = true)
	String ip() default "192.168.8.101";
	
	@AttributeDefinition(name = "Minimum power", description = "Minimum current of the Charger in mA.", required = true)
	int minHwCurrent() default 6000;

	String webconsole_configurationFactory_nameHint() default "EVCS Hardy Barth [{id}]";

}