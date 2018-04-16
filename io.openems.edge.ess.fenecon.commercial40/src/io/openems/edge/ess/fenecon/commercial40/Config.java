package io.openems.edge.ess.fenecon.commercial40;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition()
@interface Config {
	String id();

	boolean enabled();
}