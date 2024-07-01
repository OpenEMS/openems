package io.openems.edge.simulator.ess.symmetric.reacting;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.sum.GridMode;

@ObjectClassDefinition(//
		name = "Simulator EssSymmetric Reacting", //
		description = "This simulates a 'reacting' symmetric Energy Storage System.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ess0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Max Apparent Power [VA]")
	int maxApparentPower() default 10000;

	@AttributeDefinition(name = "Capacity [Wh]")
	int capacity() default 10000;

	@AttributeDefinition(name = "Initial State of Charge [%]")
	int initialSoc() default 50;

	@AttributeDefinition(name = "Grid mode")
	GridMode gridMode() default GridMode.ON_GRID;

	String webconsole_configurationFactory_nameHint() default "Simulator EssSymmetric Reacting [{id}]";
}