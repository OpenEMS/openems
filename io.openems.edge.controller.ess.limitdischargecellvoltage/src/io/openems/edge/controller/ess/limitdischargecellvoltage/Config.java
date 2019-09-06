package io.openems.edge.controller.ess.limitdischargecellvoltage;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Ess Limit Discharge Cell Voltage", //
		description = "Forces charging when cell voltage is getting to low.")
public
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlLimitTotalDischarge0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Warning Cell Voltage [mV]", description = "If voltage is below this value for a certain time period charging is forced.")
	int warningCellVoltage() default 2900;
	
	@AttributeDefinition(name = "Warning Cell Voltage time period [s]", description = "if cell voltage is lower than warning cell voltage for this time period charging is forced.")
	int warningCellVoltageTime() default 600;

	@AttributeDefinition(name = "Critical Cell Voltage [mV]", description = "Charging is forced when minimal cell voltage is below this value.")
	int criticalCellVoltage() default 2800;
	
	@AttributeDefinition(name = "Charge Power Percent [%]", description = "The charge power in percent from the maximum output of the ess", required = false)
	int chargePowerPercent() default 20;
	
	@AttributeDefinition(name = "Charge Power Time [s]", description = "Defines how long force charging is executed in seconds")
	int chargingTime() default 600;

	String webconsole_configurationFactory_nameHint() default "Controller Ess Limit Total Discharge [{id}]";

}