package io.openems.edge.kostal.piko.charger;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "KOSTAL PIKO PV-Charger", //
		description = "The PV charger implementation of a KOSTAL PIKO.")
@interface Config {
	String id() default "charger0";

	String core_id() default "kostalPiko0";

	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "KOSTAL PIKO PV-Charger [{id}]";
}