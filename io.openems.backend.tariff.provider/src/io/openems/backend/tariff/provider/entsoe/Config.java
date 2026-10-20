package io.openems.backend.tariff.provider.entsoe;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.BiddingZone;

@ObjectClassDefinition(//
		name = "Tariff.ENTSO-E", //
		description = "Configures the ENTSO-E Tariff Provider to fetch spot prices from ENTSO-E" //
)
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "tariffSpot0";

	@AttributeDefinition(name = "Bidding zones to fetch. If empty, all supported bidding zones are fetched.")
	BiddingZone[] biddingZones() default {};

	@AttributeDefinition(name = "Security Token", description = "Security token for the ENTSO-E Transparency Platform", type = AttributeType.PASSWORD, required = false)
	String securityToken() default "";

	@AttributeDefinition(name = "Service Ranking", description = "Service ranking; higher values are preferred.")
	int service_ranking() default 80;

	String webconsole_configurationFactory_nameHint() default "Tariff ENTSO-E";
}