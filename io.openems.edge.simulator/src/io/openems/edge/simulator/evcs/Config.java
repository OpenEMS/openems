package io.openems.edge.simulator.evcs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Simulator EVCS", //
		description = "This simulates a Electric Vehicle Charging Station using data provided by a data source.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcs0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Maximum power", description = "Maximum power of the charger in Watt.", required = true)
	int maxHwPower() default 22080;

	@AttributeDefinition(name = "Minimum power", description = "Minimum power of the charger in Watt.", required = true)
	int minHwPower() default 4140;

	String webconsole_configurationFactory_nameHint() default "Simulator EVCS [{id}]";

}