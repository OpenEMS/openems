package io.openems.edge.ess.core.power;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.ess.power.api.SolverStrategy;

/**
 * Configures the Power solver.
 */
@ObjectClassDefinition(//
		name = "ESS Power", //
		description = "This component solves Power distribution among energy storage systems.")
@interface Config {
	@AttributeDefinition(name = "Strategy", description = "The strategy for solving power distribution.")
	SolverStrategy strategy() default SolverStrategy.OPTIMIZE_BY_MOVING_TOWARDS_TARGET;

	@AttributeDefinition(name = "Symmetric Mode", description = "Keeps asymmetric ESS phases symmetric")
	boolean symmetricMode() default PowerComponent.DEFAULT_SYMMETRIC_MODE;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default PowerComponent.DEFAULT_DEBUG_MODE;

	String webconsole_configurationFactory_nameHint() default "ESS Power";
}