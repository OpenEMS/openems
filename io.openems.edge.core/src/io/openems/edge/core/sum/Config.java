package io.openems.edge.core.sum;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core Sum", //
		description = "The global OpenEMS Summary data.")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Core Sum";

}