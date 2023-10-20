package io.openems.edge.timeofusetariff.entsoe;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

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

	@AttributeDefinition(name = "Security Token", description = "Security token for the ENTSO-E Transparency Platform", type = AttributeType.PASSWORD)
	String securityToken() default "";

	@AttributeDefinition(name = "Bidding Zone", description = "Zone corresponding to the customer's location")
	BiddingZone biddingZone();

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff ENTSO-E [{id}]";
}