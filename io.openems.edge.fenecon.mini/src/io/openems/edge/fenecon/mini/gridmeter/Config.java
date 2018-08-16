package io.openems.edge.fenecon.mini.gridmeter;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "FENECON MINI Grid-Meter", //
		description = "The grid-meter implementation of a FENECON MINI.")
@interface Config {
	String service_pid();

	String id() default "meter0";

	String core_id() default "feneconMini0";

	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "Fenecon Mini Grid-Meter [{id}]";
}