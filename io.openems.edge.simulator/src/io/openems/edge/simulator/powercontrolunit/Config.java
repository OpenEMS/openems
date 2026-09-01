package io.openems.edge.simulator.powercontrolunit;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Simulator PCU", //
		description = "This simulates a PCU Unit. Provide a maximum BuyFromGrid/SellToGrid Limit")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "pcu0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Grid Buy Limit", description = "Grid Buy Limit in Watts")
	long buyFromGridLimit() default 100_000;

	@AttributeDefinition(name = "Sell To Limit", description = "Sell To Grid Limit in Watts")
	long sellToGridLimit() default 100_000;

	String webconsole_configurationFactory_nameHint() default "Simulator PCU [{id}]";

}