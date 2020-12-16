package io.openems.edge.ess.mr.gridcon.controller.balancing;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.ess.mr.gridcon.enums.BalancingMode;

@ObjectClassDefinition(//
		name = "MR Gridcon Controller Set Balancing Mode", //
		description = "Sets the balancing mode for the gridcon.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlSetBalancingMode0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Gridcon-ID", description = "ID of gridcon device.")
	String gridcon_id() default "gridcon0";

	@AttributeDefinition(name = "Balancing mode", description = "Balancing Mode for Gridon")
	BalancingMode balancingMode() default BalancingMode.DISABLED;

	String webconsole_configurationFactory_nameHint() default "MR Gridcon Controller Set Balancing Mode [{id}]";
}