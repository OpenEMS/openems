package io.openems.edge.ess.fenecon.openemsv1;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition()
@interface Config {
	String id();

	boolean enabled() default true;
}