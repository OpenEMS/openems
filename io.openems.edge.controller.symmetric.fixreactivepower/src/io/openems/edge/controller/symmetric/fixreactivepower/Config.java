package io.openems.edge.controller.symmetric.fixreactivepower;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.AttributeDefinition;;

@ObjectClassDefinition( //
		name = "Controller Fix Reactive Power Symmetric", //
		description = "Defines a fixed reactive power to a symmetric energy storage system.")
@interface Config {
	String id() default "ctrlFixReactivePower0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Charge/Discharge power [var]", description = "Negative values for Charge; positive for Discharge")
	int power();

	String webconsole_configurationFactory_nameHint() default "Controller Fix Reactive Power Symmetric [{id}]";
}