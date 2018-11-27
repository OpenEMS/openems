package io.openems.edge.simulator.battery;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "BMS Simulated", //
		description = "Implements a simulated battery management system that sends values given in the configuration")
@interface Config {
	String service_pid();

	String id() default "bms0";

	boolean enabled() default true;
	
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
	
	int maximalPower_W() default 50000;
	
	String webconsole_configurationFactory_nameHint() default "BMS Simulated [{id}]";


}
