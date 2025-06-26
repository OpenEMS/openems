package io.openems.edge.controller.asymmetric.fixreactivepower;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Fix Reactive Power Asymmetric", //
		description = "Defines a fixed charge/discharge power to an asymmetric energy storage system.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlFixReactivePower0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Charge/Discharge power [var] on L1", description = "Negative values for Charge; positive for Discharge")
	int powerL1();

	@AttributeDefinition(name = "Charge/Discharge power [var] on L2", description = "Negative values for Charge; positive for Discharge")
	int powerL2();

	@AttributeDefinition(name = "Charge/Discharge power [var] on L3", description = "Negative values for Charge; positive for Discharge")
	int powerL3();

	String webconsole_configurationFactory_nameHint() default "Controller Fix Reactive Power Asymmetric [{id}]";
}