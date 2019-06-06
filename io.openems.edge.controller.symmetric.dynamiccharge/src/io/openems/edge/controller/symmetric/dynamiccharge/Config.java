package io.openems.edge.controller.symmetric.dynamiccharge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Dynamic Charge", //
		description = "controller which schedules the charge and discharge according to the different hourly prices of electricity")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlDynamicCharge0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Read-Only mode", description = "Enables Read-Only mode and disables the charging of the battery")
	boolean readonly() default false;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Grid-Meter-Id", description = "ID of the Grid-Meter.")
	String meter_id();

	@AttributeDefinition(name = "Morning-Hour", description = "Calculation stop at this hour")
	int Max_Morning_hour() default 7;

	@AttributeDefinition(name = "Evening-Hour", description = "Calculation starts at this hour")
	int Max_Evening_hour() default 16;

	String webconsole_configurationFactory_nameHint() default "Controller Dynamic Charge Symmetric [{id}]";
}