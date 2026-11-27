package io.openems.edge.timeofusetariff.spottariff.gridbuy;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.BiddingZone;

@ObjectClassDefinition(//
		name = "Time-Of-Use Tariff Spot Tariff", //
		description = "Time-Of-Use Tariff implementation that uses the Spot Tariff to calculate prices.")
@interface Config {
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timeOfUseTariff0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Bidding Zone", description = "Zone corresponding to the customer's location")
	BiddingZone biddingZone();

	@AttributeDefinition(name = "Ancillary Costs JSON", description = "Ancillary Costs in JSON format [Currency/MWh]")
	String ancillaryCosts();

	@AttributeDefinition(name = "Mathematical expression to calculate gross price", description = "[x] is the EPEX price in [Currency/MWh], [y] the ancillary cost per period; defaults to \"x + y\"")
	String calculateExpression() default "";

	@AttributeDefinition(name = "Service Ranking", description = "Service ranking; higher values are preferred.")
	int service_ranking() default 10;

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff Spot Tariff [{id}]";
}
