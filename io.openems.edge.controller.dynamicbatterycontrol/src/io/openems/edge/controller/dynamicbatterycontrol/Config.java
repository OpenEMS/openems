package io.openems.edge.controller.dynamicbatterycontrol;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
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

	@AttributeDefinition(name = "url", description = "URL to connect API", type = AttributeType.STRING)
	String url() default "https://portal.blogpv.net/api/bci/signal";
	
	@AttributeDefinition(name = "Start-Hour", description = "Fallback start hour if no pv.")
	int maxStratHour() default 9;

	@AttributeDefinition(name = "End-Hour", description = "fallback end hour if no pv.")
	int maxEndHour() default 17;
	
	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode (Displays the Predicted Energy Values in the Log")
	boolean debugMode() default false;

	String webconsole_configurationFactory_nameHint() default "Controller Dynamic Battery-Control [{id}]";

}