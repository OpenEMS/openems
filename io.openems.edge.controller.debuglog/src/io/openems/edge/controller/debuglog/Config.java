package io.openems.edge.controller.debuglog;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition()
@interface Config {
	String id();

	boolean enabled();
}