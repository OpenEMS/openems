package io.openems.edge.controller.dischargelimitconsideringcellvoltage;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Discharge Limit Considering Cell Voltage", //
		description = "Limits discharge for a battery considering the minimal cell voltage.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlDischargeLimitConsideringCellVoltage0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Battery-ID", description = "ID of battery device.")
	String battery_id();

	@AttributeDefinition(name = "Min-SoC", description = "Discharging is blocked while State of Charge is below Min-SoC.")
	int minSoc() default 5;

	@AttributeDefinition(name = "Charge-SoC", description = "Charging until State of Charge has reached Charge-SoC.")
	int chargeSoc() default 3;

	@AttributeDefinition(name = "Minimal total voltage", description = "Charging is forced if system voltage is lower than this value")
	int minimalTotalVoltage() default 675;

	@AttributeDefinition(name = "First cell voltage limit", description = "Charging is forced if minimal cell voltage is lower then this value for a certain time period")
	float firstCellVoltageLimit() default 2.85f;

	@AttributeDefinition(name = "First limit time span", description = "Charging is forced if min cell voltage is lower than first limit longer than this time period in seconds")
	int timeSpan() default 600;

	@AttributeDefinition(name = "Second cell voltage limit", description = "Charging is forced if minimal cell voltage is lower than this value")
	float secondCellVoltageLimit() default 2.8f;

	String webconsole_configurationFactory_nameHint() default "Controller Discharge Limit Considering Minimal Cell Voltage [{id}]";
}