package io.openems.edge.fenecon.mini.pvmeter;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "FENECON MINI Pv-Meter", //
		description = "The pv-meter implementation of a FENECON MINI.")
@interface Config {
	String service_pid();

	String id() default "pvmeter0";

	String core_id() default "feneconMini0";

	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "Fenecon Mini Pv-Meter [{id}]";
}