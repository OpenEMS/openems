package io.openems.edge.controller.ortsnetzauslastung;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.DebugMode;

@ObjectClassDefinition(//
		name = "Controller Ortsnetz-Auslastung", //
		description = "Sends periodic grid voltage measurements to ortsnetz-auslastung.de")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlOrtsnetzAuslastung0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "URL", description = "URL of the Ortsnetz-Auslastung measurements API")
	String url() default "https://www.ortsnetz-auslastung.de/v1/measurements";

	@AttributeDefinition(name = "Meter-ID", description = "ID of the Electricity-Meter, that measures the grid connection point.")
	String meter_id();

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	DebugMode debugMode() default DebugMode.OFF;

	String webconsole_configurationFactory_nameHint() default "Controller Ortsnetz-Auslastung [{id}]";
}
