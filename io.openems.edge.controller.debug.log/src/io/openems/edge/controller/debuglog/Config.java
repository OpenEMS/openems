package io.openems.edge.controller.debuglog;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Debug Log", //
		description = "This controller prints information about all available components on the console")
@interface Config {
	String id() default "ctrlDebugLog0";

	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "Controller Debug Log [{id}]";
}