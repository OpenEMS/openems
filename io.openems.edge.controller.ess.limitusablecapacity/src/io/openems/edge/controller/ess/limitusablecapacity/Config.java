package io.openems.edge.controller.ess.limitusablecapacity;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Ess Limit Total Usable capacity", //
		description = "Limits the usable capacity for an ess")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlLimitUsableCapacity0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id() default "ess0";

	@AttributeDefinition(name = "Stop-Discharge-Soc", description = "Stopping discharge Soc")
	int stopDischargeSoc() default 10;

	@AttributeDefinition(name = "Allow-Discharge-Soc", description = "Allow Discharge Soc")
	int allowDischargeSoc() default 12;

	@AttributeDefinition(name = "Force-Charge-Soc", description = "Force charge Soc")
	int forceChargeSoc() default 8;

	@AttributeDefinition(name = "Stop-Charge-Soc", description = "Stop Charge Soc")
	int stopChargeSoc() default 90;

	@AttributeDefinition(name = "Allow Charge Soc", description = "Allow Charge Soc")
	int allowChargeSoc() default 85;

	String webconsole_configurationFactory_nameHint() default "Controller Ess Limit Usable Capacity [{id}]";

}