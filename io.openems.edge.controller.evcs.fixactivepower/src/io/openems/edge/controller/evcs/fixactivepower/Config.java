package io.openems.edge.controller.evcs.fixactivepower;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Electric Vehicle Charging Station: Fix Active Power", //
		description = "Defines a fixed charge/discharge power to an Electric Vehicle Charging Station.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlEvcsFixActivePower0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Evcs-id", description = "ID of Evcs device.")
	String evcs_id();

	@AttributeDefinition(name = "Charge power [W]", description = "Fix value that should be charged")
	int power();
	
	@AttributeDefinition(name = "Update Frequency [s]", description = "Timeout to write the value to the EVCS. Recommended minimum: 60s or more for optimal settings.")
	int updateFrequency() default 60;

	String webconsole_configurationFactory_nameHint() default "Controller Electric Vehicle Charging Station: Fix Active Power [{id}]";

}
