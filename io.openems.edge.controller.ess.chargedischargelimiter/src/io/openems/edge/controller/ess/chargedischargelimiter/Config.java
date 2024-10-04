package io.openems.edge.controller.ess.chargedischargelimiter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller ESS Charge Discharge Limiter", //
		description = "Limits total charge and discharge for an Ess.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrl.ChargeDischargelimiter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Min SoC [%]", description = "Discharging is blocked while State of Charge is below Min-SoC.")
	int minSoc() default 15;

	@AttributeDefinition(name = "Force-Charge SoC [%]", description = "Charging is forced while State of Charge is below Force-Charge-SoC.")
	int forceChargeSoc() default 10;
	
	@AttributeDefinition(name = "Force-Charge Power [W]", description = "The charge power during force-charging. If parameter is left empty," //
			+ "zero or negative, this value is calculated from Max-Charge-Power divided by 5", required = false)
	int forceChargePower();
	
	@AttributeDefinition(name = "Max SoC [%]", description = "Charging is blocked while State of Charge is above Max-SoC.")
	int maxSoc() default 85;

	@AttributeDefinition(name = "Force-Discharge SoC [%]", description = "Discharging is forced while State of Charge is above Force-Discharge-SoC.")
	int forceDischargeSoc() default 90;

	@AttributeDefinition(name = "Force-Discharge Power [W]", description = "The discharge power during force-discharging. If parameter is left empty," //
			+ "zero or negative, this value is calculated from Min-Charge-Power divided by 5", required = false)
	int forceDischargePower();

	String webconsole_configurationFactory_nameHint() default "Controller io.openems.edge.controller.ess.chargedischargelimiter [{id}]";

}