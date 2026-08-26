package io.openems.edge.timeofusetariff.entsoe.gridsell;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.EntsoeBiddingZone;

@ObjectClassDefinition(//
		name = "Tariff ENTSO-E Grid-Sell", //
		description = "Implementation of a dynamic grid-sell tariff using market prices from the ENTSO-E transparency platform.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "tariffGridSell0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Security Token", description = "Security token for the ENTSO-E Transparency Platform", type = AttributeType.PASSWORD, required = false)
	String securityToken() default "";

	@AttributeDefinition(name = "Bidding Zone", description = "Zone corresponding to the location")
	EntsoeBiddingZone biddingZone();

	@AttributeDefinition(name = "Mathematical expression to calculate gross price", description = "[x] is the EPEX price in [Currency/MWh]; defaults to \"x\"")
	String calculateExpression() default "";

	String webconsole_configurationFactory_nameHint() default "Tariff ENTSO-E Grid-Sell [{id}]";
}