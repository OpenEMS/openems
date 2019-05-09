package io.openems.edge.controller.symmetric.dynamiccharge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Dynamic Charge", //
		description = "controller which schedules the charges according to the dynamic hourly prices of electricity")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlDynamicCharge0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();
	
	@AttributeDefinition(name = "Morning-Hour", description = "Calculation stop at this hour")
	int Max_Morning_hour() default 7;
	
	@AttributeDefinition(name = "Evening-Hour", description = "Calculation starts at this hour")
	int Max_Evening_hour() default 4;
	
	@AttributeDefinition(name = "Grid-Meter-Id", description = "ID of the Grid-Meter.")
	String meter_id();

//	@AttributeDefinition(name = "Charge/Discharge power [W]", description = "Negative values for Charge; positive for Discharge")
//	int power();

	String webconsole_configurationFactory_nameHint() default "Controller Dynamic Charge Symmetric [{id}]";
}