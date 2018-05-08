package io.openems.edge.common.user;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Core User", //
		description = "This component handles User authentication.")
@interface Config {
	String service_pid();

	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "Core User [{id}]";
}