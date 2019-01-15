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

	@AttributeDefinition(name = "Minimum power [W]", description = "Set the minimum power in Watt.")
	int minPower();

	@AttributeDefinition(name = "Charge-Mode", description = "Set the charge-mode.")
	ChargeMode chargeMode();

	String webconsole_configurationFactory_nameHint() default "Controller Electric Vehicle Charging Station [{id}]";

}