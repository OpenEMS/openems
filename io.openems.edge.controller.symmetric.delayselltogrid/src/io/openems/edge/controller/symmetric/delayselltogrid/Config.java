package io.openems.edge.controller.symmetric.delayselltogrid;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Delay-Sell-To-Grid Symmetric", //
		description = "Calculate power peaks and discharges the battery in high consumption periods or calculates the exceeds charge power to charge the battery.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlDelaySellToGrid0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Grid-Meter-ID", description = "ID of the Grid-Meter.")
	String meter_id();

	@AttributeDefinition(name = "Delay Sell To Grid power", description = "Pv power under this value is considered a peak and cover to this value.")
	int delaySellToGridPower();
	

	@AttributeDefinition(name = "Charge Power Start Point", description = "Pv power above this value starts to charge the system.")
	int chargePower();

	String webconsole_configurationFactory_nameHint() default "Controller Delay Sell To Grid Symmetric [{id}]";
}