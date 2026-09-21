package io.openems.edge.timeofusetariff.entsoe.gridbuy;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.EntsoeBiddingZone;

@ObjectClassDefinition(//
		name = "Time-Of-Use Tariff ENTSO-E", //
		description = "Time-Of-Use Tariff implementation that uses the ENTSO-E transparency platform.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timeOfUseTariff0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Security Token", description = "Security token for the ENTSO-E Transparency Platform", type = AttributeType.PASSWORD, required = false)
	String securityToken() default "";

	@AttributeDefinition(name = "Bidding Zone", description = "Zone corresponding to the customer's location")
	EntsoeBiddingZone biddingZone();

	@AttributeDefinition(name = "Ancillary Costs JSON", description = "Ancillary Costs in JSON format [Currency/MWh]")
	String ancillaryCosts();

	@AttributeDefinition(name = "Mathematical expression to calculate gross price", description = "[x] is the EPEX price in [Currency/MWh], [y] the ancillary cost per period; defaults to \"x + y\"")
	String calculateExpression() default "";

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff ENTSO-E [{id}]";
}