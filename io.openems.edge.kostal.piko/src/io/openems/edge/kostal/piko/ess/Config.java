package io.openems.edge.kostal.piko.ess;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "KOSTAL PIKO ESS", //
		description = "The energy storage system implementation of a KOSTAL PIKO.")
@interface Config {
	String id() default "ess0";

	String core_id() default "kostalPiko0";

	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "KOSTAL PIKO ESS [{id}]";
}