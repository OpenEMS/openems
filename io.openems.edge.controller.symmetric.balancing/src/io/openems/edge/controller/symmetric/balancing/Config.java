package io.openems.edge.controller.symmetric.balancing;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Balancing Symmetric", //
		description = "Optimizes the self-consumption by keeping the grid meter on zero.")
@interface Config {

	String id() default "ctrlBalancing0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Grid-Meter-ID", description = "ID of the Grid-Meter.")
	String meter_id();

	String webconsole_configurationFactory_nameHint() default "Controller Balancing Symmetric [{id}]";

}