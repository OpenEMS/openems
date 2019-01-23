package io.openems.edge.controller.symmetric.limitactivepower;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Limit Active Power Symmetric", //
		description = "Defines charge and discharge limits for a symmetric energy storage system.")
@interface Config {
	String id() default "ctrlLimitActivePower0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Max Charge Power [W]", description = "Positive value describing the maximum Charge Power.")
	int maxChargePower();
	
	@AttributeDefinition(name = "Max Discharge Power [W]", description = "Positive value describing the maximum Discharge Power.")
	int maxDischargePower();

	String webconsole_configurationFactory_nameHint() default "Controller Limit Active Power Symmetric [{id}]";
}