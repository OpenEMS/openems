package io.openems.edge.controller.evcs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Electric Vehicle Charging Station", //
		description = "Limits the maximum charging power of an electric vehicle charging station.")
@interface Config {

	String id() default "ctrlEvcs0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Evcs-ID", description = "ID of Evcs device.")
	String evcs_id();

	@AttributeDefinition(name = "Charge-Mode", description = "Set the charge-mode.")
	ChargeMode chargeMode();
	
	@AttributeDefinition(name = "Force-charge minimum power [W]", description = "Set the minimum power for the force charge mod in Watt.")
	int forceChargeMinPower();
	
	@AttributeDefinition(name = "Default-charge minimum power [W]", description = "Set the minimum power for the default charge mod in Watt.")
	int defaultChargeMinPower();
	
	String webconsole_configurationFactory_nameHint() default "Controller Electric Vehicle Charging Station [{id}]";

}