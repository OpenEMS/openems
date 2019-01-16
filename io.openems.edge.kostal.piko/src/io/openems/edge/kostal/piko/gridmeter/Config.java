package io.openems.edge.kostal.piko.gridmeter;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "KOSTAL PIKO Grid-Meter", //
		description = "The grid-meter implementation of a KOSTAL PIKO.")
@interface Config {
	String id() default "meter0";

	String core_id() default "kostalPiko0";

	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "KOSTAL PIKO Grid-Meter [{id}]";
}