package io.openems.edge.controller.pvinverter.selltogridlimit;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller PV-Inverter Sell-to-Grid Limit", //
		description = "Reduces PV-Inverter power to limit the Sell-to-Grid power.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlPvInverterSellToGridLimit0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "PV-Inverter-ID", description = "ID of PV-Inverter device.")
	String pvInverter_id();

	@AttributeDefinition(name = "Grid-Meter-ID", description = "ID of the Grid-Meter.")
	String meter_id();

	@AttributeDefinition(name = "Asymmetric Mode (Not optimized for Single-Phase PV-Inverter)", description = "Reduces PV-Inverter power to limit the Sell-to-Grid power, depending on the individual phase.")
	boolean asymmetricMode() default false;

	@AttributeDefinition(name = "Maximum allowed Sell-To-Grid power (per Phase in asymmetric mode)", description = "The target limit for sell-to-grid power.")
	int maximumSellToGridPower() default 5_000;

	String webconsole_configurationFactory_nameHint() default "Controller PV-Inverter Sell-to-Grid Limit [{id}]";

}