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

	@AttributeDefinition(name = "ENTSO-E Security Token", description = "Security token for the ENTSO-E Transparency Platform", type = AttributeType.PASSWORD)
	String securityToken();

	@AttributeDefinition(name = "Exchange Rate access key", description = "Access key for the Exchange rate host API, Please log into https://exchangerate.host/ to get personal access key", type = AttributeType.PASSWORD)
	String accessKey();

	@AttributeDefinition(name = "Bidding Zone", description = "Zone corresponding to the customer's location")
	BiddingZone biddingZone();

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff ENTSO-E [{id}]";
}