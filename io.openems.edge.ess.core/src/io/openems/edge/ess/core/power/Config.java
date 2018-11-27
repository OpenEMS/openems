package io.openems.edge.ess.core.power;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configures the Power solver.
 */
@ObjectClassDefinition( //
		name = "ESS Power", //
		description = "This component solves Power distribution among energy storage systems.")
@interface Config {
	String service_pid();

	@AttributeDefinition(name = "Symmetric Mode", description = "Keeps asymmetric ESS phases symmetric")
	boolean symmetricMode() default PowerComponent.DEFAULT_SYMMETRIC_MODE;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default PowerComponent.DEFAULT_DEBUG_MODE;

	String webconsole_configurationFactory_nameHint() default "ESS Power";
}