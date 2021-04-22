package io.openems.edge.core.meta;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core Meta", //
		description = "The global OpenEMS manager for Metadata.")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Core Meta";

}