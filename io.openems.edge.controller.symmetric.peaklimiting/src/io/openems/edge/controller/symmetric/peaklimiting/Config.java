package io.openems.edge.controller.symmetric.peaklimiting;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;



@ObjectClassDefinition(//
		name = "Controller Peak Limiting", //
		description = "Compensates the Feed In Power due to Energy Provider Limitations.")

@interface Config {
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlPeakLimiting0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Use simple Mode", description = "Use simple mode.")
	boolean simple() default false;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();
	
	@AttributeDefinition(name = "Grid-Meter-ID", description = "ID of the Grid-Meter.")
	String grid_meter_id();

	@AttributeDefinition(name = "Maximum SOC Load", description = "The maxmim SOC value (%) to hold before compensating the peaks. ")
	int maxSOC() default 80;

	@AttributeDefinition(name = "Max Grid Sell Power", description = "The maximum Power to sell to grid when compensating the peaks. 0 = No maximum.")
	int maxPower() default 0;

	String webconsole_configurationFactory_nameHint() default "Controller Peak Limiting Symmetric [{id}]";
}
