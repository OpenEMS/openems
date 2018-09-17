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

	@AttributeDefinition(name = "Solve duration limit", description = "Solving a problem is limited to X milliseconds.")
	int solveDurationLimit() default PowerComponent.DEFAULT_SOLVE_DURATION_LIMIT;

	@AttributeDefinition(name = "Symmetric Mode", description = "Keeps asymmetric ESS phases symmetric")
	boolean symmetricMode() default PowerComponent.DEFAULT_SYMMETRIC_MODE;
	
	String webconsole_configurationFactory_nameHint() default "ESS Power";
}