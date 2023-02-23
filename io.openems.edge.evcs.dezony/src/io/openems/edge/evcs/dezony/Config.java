package io.openems.edge.evcs.dezony;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "EVCS dezony IQ", //
		description = "Implements the dezony - electric vehicle charging station.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcs0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the charging station. If the charger has two connectors, the second/slave evcs has the IP 192.168.25.31.", required = true)
	String ip() default "192.168.25.30";

	@AttributeDefinition(name = "Minimum hardware current", description = "Minimum current of the Charger in mA.", required = true)
	int minHwCurrent() default 6000;

	@AttributeDefinition(name = "Maximum hardware current", description = "Maximum current of the Charger in mA.", required = true)
	int maxHwCurrent() default 32000;

	String webconsole_configurationFactory_nameHint() default "EVCS dezony [{id}]";

}