package io.openems.edge.core.sum;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core Sum", //
		description = "The global OpenEMS Summary data.")
@interface Config {

	@AttributeDefinition(name = "Ignore Component Warnings/Faults", description = "Component-IDs for which Fault or Warning Channels should be ignored.")
	String[] ignoreStateComponents() default {};

	String webconsole_configurationFactory_nameHint() default "Core Sum";

}