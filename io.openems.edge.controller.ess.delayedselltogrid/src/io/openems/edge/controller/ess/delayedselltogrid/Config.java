package io.openems.edge.controller.ess.delayedselltogrid;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Ess Delayed-Sell-To-Grid ", //
		description = "This controller discharges the battery with the amount of difference between meter power configured \"delayedSellToGridPower\" when meter active power less than. "
				+ "		This behaviour can be inverse direction(charging the battery) when meter active power exceeds the \"chargePower\" limit.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlDelayedSellToGrid0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Grid-Meter-ID", description = "ID of the Grid-Meter.")
	String meter_id();

	@AttributeDefinition(name = "Delayed Sell To Grid power", description = "The limit at which the discharge event begins...")
	int delayedSellToGridPower();
	
	@AttributeDefinition(name = "Charge Power Start Point", description = "The limit at which the charge event begins....")
	int chargePower();

	String webconsole_configurationFactory_nameHint() default "Controller Ess Delayed Sell To Grid [{id}]";
}