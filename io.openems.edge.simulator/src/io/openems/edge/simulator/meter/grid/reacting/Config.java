package io.openems.edge.simulator.meter.grid.reacting;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Simulator GridMeter Reacting", //
		description = "This simulates an 'reacting' Grid meter.")
@interface Config {
	String id() default "meter0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Minimum Ever Active Power", description = "This is automatically updated.")
	int minActivePower();

	@AttributeDefinition(name = "Maximum Ever Active Power", description = "This is automatically updated.")
	int maxActivePower();

	String webconsole_configurationFactory_nameHint() default "Simulator GridMeter Reacting [{id}]";
}