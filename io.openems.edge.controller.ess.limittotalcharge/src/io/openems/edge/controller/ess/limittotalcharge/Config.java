package io.openems.edge.controller.ess.limittotalcharge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Ess Limit Total Charge", //
		description = "Limits total charge for an Ess.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlLimitTotalCharge0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Hybrid ESS Mode", description = "TARGET_AC limits inverter power (legacy). TARGET_DC limits battery power and accounts for local PV production.")
	HybridEssMode hybridEssMode() default HybridEssMode.TARGET_AC;

	@AttributeDefinition(name = "Max SoC [%]", description = "Charging is blocked while State of Charge is above Max-SoC.")
	int maxSoc() default 85;

	@AttributeDefinition(name = "Force-Discharge SoC [%]", description = "Discharging is forced while State of Charge is above Force-Discharge-SoC.")
	int forceDischargeSoc() default 90;

	@AttributeDefinition(name = "Force-Discharge Power [W]", description = "The discharge power during force-discharging. If parameter is left empty," //
			+ "zero or negative, this value is calculated from Max-Discharge-Power divided by 5", required = false)
	int forceDischargePower();

	String webconsole_configurationFactory_nameHint() default "Controller Ess Limit Total Charge [{id}]";

}
