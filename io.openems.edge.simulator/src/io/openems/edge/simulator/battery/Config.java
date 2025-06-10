package io.openems.edge.simulator.battery;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Simulator Battery", //
		description = "Implements a simulated battery that sends values given in the configuration")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "bms0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Number of slaves", description = "The number of slaves in this battery rack (max. 20)", min = "1", max = "20")
	int numberOfSlaves() default 10;

	int disChargeMinVoltage();

	int chargeMaxVoltage();

	int disChargeMaxCurrent();

	int chargeMaxCurrent();

	int soc() default 50;

	int soh() default 95;

	int temperature() default 30;

	int capacityKWh() default 50;

	int voltage() default 700;

	int minCellVoltage_mV() default 3300;

	String webconsole_configurationFactory_nameHint() default "Simulator Battery [{id}]";

}
