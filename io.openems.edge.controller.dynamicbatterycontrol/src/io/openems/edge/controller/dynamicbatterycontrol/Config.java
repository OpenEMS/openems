package io.openems.edge.controller.dynamicbatterycontrol;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Dynamic Battery-Control", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlDynamicBatteryControl0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Grid-Meter-Id", description = "ID of the Grid-Meter.")
	String meter_id();
	
	@AttributeDefinition(name = "Source", description = "Url to the API or A CSV-Input containing an optional title line and a series of values.")
	Source source();

	@AttributeDefinition(name = "Start-Hour", description = "Fallback start hour if no pv.")
	int maxStartHour() default 9;

	@AttributeDefinition(name = "End-Hour", description = "fallback end hour if no pv.")
	int maxEndHour() default 17;

	String webconsole_configurationFactory_nameHint() default "Controller Dynamic Battery-Control [{id}]";

}