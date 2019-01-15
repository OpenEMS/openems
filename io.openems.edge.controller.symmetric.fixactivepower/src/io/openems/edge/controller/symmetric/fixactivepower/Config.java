package io.openems.edge.controller.symmetric.fixactivepower;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Fix Active Power Symmetric", //
		description = "Defines a fixed charge/discharge power to a symmetric energy storage system.")
@interface Config {
	String id() default "ctrlFixActivePower0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Charge/Discharge power [W]", description = "Negative values for Charge; positive for Discharge")
	int power();

	String webconsole_configurationFactory_nameHint() default "Controller Fix Active Power Symmetric [{id}]";
}