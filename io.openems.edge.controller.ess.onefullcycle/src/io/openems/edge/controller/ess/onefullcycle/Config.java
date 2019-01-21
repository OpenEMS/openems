package io.openems.edge.controller.ess.onefullcycle;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Ess One Full Cycle", //
		description = "Completes one full cycle for an Ess.")
@interface Config {
	String id() default "ctrlOneFullCycle0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Power [W]", description = "Charge/discharge power")
	int power();

	String webconsole_configurationFactory_nameHint() default "Controller Ess One Full Cycle [{id}]";
}