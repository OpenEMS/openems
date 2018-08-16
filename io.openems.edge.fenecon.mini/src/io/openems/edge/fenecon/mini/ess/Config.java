package io.openems.edge.fenecon.mini.ess;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "FENECON MINI ESS", //
		description = "The energy storage system implementation of a FENECON MINI.")
@interface Config {
	String service_pid();

	String id() default "ess0";

	String core_id() default "feneconMini0";

	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "FENECON MINI ESS [{id}]";
}