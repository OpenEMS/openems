package io.openems.edge.ess.kostal.piko;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "ESS KOSTAL PIKO", //
		description = "Implements the KOSTAL Piko energy storage system.")
@interface Config {
	String service_pid();

	String id() default "ess0";

	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "ESS KOSTAL Piko [{id}]";
}