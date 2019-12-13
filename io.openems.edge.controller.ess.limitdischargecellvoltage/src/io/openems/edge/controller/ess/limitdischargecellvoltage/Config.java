package io.openems.edge.controller.ess.limitdischargecellvoltage;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Ess Limit Discharge Cell Voltage", //
		description = "Forces charging or stops discharging when cell limits are reached." //
)
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlLimitDischargeCellVoltage0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Warning Low Cell Voltage [mV]", description = "If voltage is below this value discharging is stopped.")
	int warningLowCellVoltage() default 3000;

	@AttributeDefinition(name = "Critical Low Cell Voltage [mV]", description = "Charging is forced when minimal cell voltage is below this value.")
	int criticalLowCellVoltage() default 2900;

	@AttributeDefinition(name = "Critical High Cell Voltage [mV]", description = "Charging is stopped when minimal cell voltage is above this value.")
	int criticalHighCellVoltage() default 3650;

	@AttributeDefinition(name = "Warning SoC [%]", description = "If SoC is below this value discharging is stopped.")
	int warningSoC() default 10;

	@AttributeDefinition(name = "Critical SoC [%]", description = "Charging is forced when SoC is below this value.")
	int criticalSoC() default 5;

	@AttributeDefinition(name = "Delta SoC [%]", description = "Defines how far the SoC has to increase until discharging is allowed.")
	int deltaSoC() default 5;

	@AttributeDefinition(name = "Low temperature [°C]", description = "If min cell temperature is below this temperature charging/discharging is stopped.")
	int lowTemperature() default 0;

	@AttributeDefinition(name = "High temperature [°C]", description = "If max cell temperature is above this temperature charging/discharging is stopped.")
	int highTemperature() default 60;

	@AttributeDefinition(name = "ForceCharge Power Percent [%]", description = "The charge power in percent from the maximum output of the ess", required = false)
	int chargePowerPercent() default 20;

	@AttributeDefinition(name = "ForceCharge Power Time [s]", description = "Defines how long force charging is executed in seconds")
	int chargingTime() default 600;

	String webconsole_configurationFactory_nameHint() default "Controller Ess Limit Total Discharge [{id}]";

}