package io.openems.edge.fenecon.mini.charger;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "FENECON MINI PV-Charger", //
		description = "The PV charger implementation of a FENECON MINI.")
@interface Config {
	String service_pid();

	String id() default "charger0";

	String core_id() default "feneconMini0";

	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "FENECON MINI PV-Charger [{id}]";
}