package io.openems.edge.energy;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.energy.api.Version;

@ObjectClassDefinition(//
		name = "Core Energy Scheduler", //
		description = "The global Energy Scheduler.")
@interface Config {

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Log-Verbosity", description = "The log verbosity.")
	LogVerbosity logVerbosity() default LogVerbosity.DEBUG_LOG;

	@AttributeDefinition(name = "Version", description = "Select version of implementation")
	Version version() default Version.V1_ESS_ONLY;

	String webconsole_configurationFactory_nameHint() default "Core Energy Scheduler";
}