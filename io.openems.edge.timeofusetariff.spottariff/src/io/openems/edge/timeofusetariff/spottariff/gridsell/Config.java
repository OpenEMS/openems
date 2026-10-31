package io.openems.edge.timeofusetariff.spottariff.gridsell;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.BiddingZone;

@ObjectClassDefinition(//
		name = "Tariff Spot Grid-Sell", //
		description = "Implementation of a dynamic grid-sell tariff using spot prices to calculate prices.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "tariffGridSell0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Bidding Zone", description = "Zone corresponding to the location")
	BiddingZone biddingZone();

	@AttributeDefinition(name = "Mathematical expression to calculate gross price", description = "[x] is the EPEX price in [Currency/MWh]; defaults to \"x\"")
	String calculateExpression() default "";

	@AttributeDefinition(name = "Service Ranking", description = "Service ranking; higher values are preferred.")
	int service_ranking() default 10;

	String webconsole_configurationFactory_nameHint() default "Tariff Spot Grid-Sell [{id}]";
}