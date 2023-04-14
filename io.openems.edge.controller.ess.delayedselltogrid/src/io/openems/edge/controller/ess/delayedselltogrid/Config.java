package io.openems.edge.controller.ess.delayedselltogrid;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Ess Delayed Sell-To-Grid ", //
		description = """
			Controls an energy storage system so, that it delays the sell-to-grid power e.g. of a photovoltaics system.\s\
			It charges the battery, when sell-to-grid power exceeds the configured "Sell-To-Grid power limit" \
			and discharges when sell-to-grid power is falling below "Continuous Sell-To-Grid power".""")
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

	@AttributeDefinition(name = "Sell-To-Grid power limit", description = "Charge the battery when this limit is exceeded.")
	int sellToGridPowerLimit();

	@AttributeDefinition(name = "Continuous Sell-To-Grid power", description = "Discharge the battery when the sell-to-grid power falls below this limit.")
	int continuousSellToGridPower();

	String webconsole_configurationFactory_nameHint() default "Controller Ess Delayed Sell-To-Grid [{id}]";
}