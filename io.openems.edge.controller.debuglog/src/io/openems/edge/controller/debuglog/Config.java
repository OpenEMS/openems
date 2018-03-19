package io.openems.edge.controller.debuglog;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition()
@interface Config {
	String service_pid();

	boolean enabled() default true;
}