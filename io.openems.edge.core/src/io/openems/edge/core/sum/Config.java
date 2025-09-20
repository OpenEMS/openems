package io.openems.edge.core.sum;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core Sum", //
		description = "The global OpenEMS Summary data.")
@interface Config {

	@AttributeDefinition(name = "Maximum ever Sell-to-Grid power [W]", description = "Range: negative or zero")
	int gridMinActivePower() default 0;

	@AttributeDefinition(name = "Maximum ever Buy-from-Grid power [W]", description = "Range: positive or zero")
	int gridMaxActivePower() default 0;

	@AttributeDefinition(name = "Maximum ever Production power [W]", description = "Includes AC- and DC-side production. Range: positive or zero")
	int productionMaxActivePower() default 0;

	@AttributeDefinition(name = "Maximum ever ESS Charge power [W]", description = "Range: negative or zero")
	int essMinDischargePower() default 0;

	@AttributeDefinition(name = "Maximum ever ESS Discharge power [W]", description = "Range: positive or zero")
	int essMaxDischargePower() default 0;

	@AttributeDefinition(name = "Maximum ever Consumption power [W]", description = "Range: positive or zero")
	int consumptionMaxActivePower() default 0;

	@AttributeDefinition(name = "Ignore Component Warnings/Faults", description = "Component-IDs for which Fault or Warning Channels should be ignored.")
	String[] ignoreStateComponents() default {};

	String webconsole_configurationFactory_nameHint() default "Core Sum";

}