package io.openems.edge.ess.core.power;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.filter.PidFilter;
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
	boolean symmetricMode() default EssPower.DEFAULT_SYMMETRIC_MODE;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default EssPower.DEFAULT_DEBUG_MODE;

	@AttributeDefinition(name = "Enable PID Filter", description = "Enables the PID Filter with the settings for P, I and D below")
	boolean enablePid() default true;

	@AttributeDefinition(name = "PID Filter: Proportional gain", description = "The weight of proportional gain in the PID filter. Value between [0;1].")
	double p() default PidFilter.DEFAULT_P;

	@AttributeDefinition(name = "PID Filter: Integral gain", description = "The weight of integral gain in the PID filter. Value between [0;1].")
	double i() default PidFilter.DEFAULT_I;

	@AttributeDefinition(name = "PID Filter: Derivative gain", description = "The weight of derivative gain in the PID filter. Value between [0;1].")
	double d() default PidFilter.DEFAULT_D;

	String webconsole_configurationFactory_nameHint() default "ESS Power";
}