package io.openems.edge.ess.fenecon.commercial40.surplusfeedin;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "ESS FENECON Commercial 40 Surplus-Feed-In-Controller", //
		description = "Enables surplus-feed-in for Commercial 40 DC system")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlCommercial40SurplusFeedIn0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of ESS device.")
	String ess_id();

	@AttributeDefinition(name = "Charger-ID", description = "ID of Charger device.")
	String charger_id();

	@AttributeDefinition(name = "State-of-Charge limit", description = "Start Surplus-feed-in if State-of-Charge is bigger than this limit")
	int socLimit() default 90;

	@AttributeDefinition(name = "Allowed-Charge-Power limit", description = "Start Surplus-feed-in if Charge-Power is smaller than this limit. (Needs to set negative)")
	int allowedChargePowerLimit() default -8_000;

	@AttributeDefinition(name = "Increase Surplus-Feed-In Power by percentage", description = "1.1 for 10 %")
	double increasePowerFactor() default 1.1;

	@AttributeDefinition(name = "Max increase Surplus-Feed-In Power", description = "")
	int maxIncreasePowerFactor() default 2_000;

	@AttributeDefinition(name = "Off Time", description = "The time to stop grid feed in.")
	String offTime() default "17:00:00";

	String webconsole_configurationFactory_nameHint() default "ESS FENECON Commercial 40 Surplus-Feed-In-Controller [{id}]";
}