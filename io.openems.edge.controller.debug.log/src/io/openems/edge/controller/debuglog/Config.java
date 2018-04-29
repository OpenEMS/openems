package io.openems.edge.controller.debuglog;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Debug Log Controller", //
		description = "This controller prints information about all available components on the console")
@interface Config {
	String service_pid();

	String id();

	boolean enabled();

	String webconsole_configurationFactory_nameHint() default "Debug Log Controller [{id}]";
}