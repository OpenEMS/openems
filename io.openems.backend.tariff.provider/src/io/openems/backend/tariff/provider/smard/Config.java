package io.openems.backend.tariff.provider.smard;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.BiddingZone;

@ObjectClassDefinition(//
		name = "Tariff.SMARD", //
		description = "Configures the SMARD Tariff Provider to fetch spot prices from SMARD" //
)
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "tariffSpot0";

	@AttributeDefinition(name = "Bidding zones to fetch. If empty, all supported bidding zones are fetched.")
	BiddingZone[] biddingZones() default {};

	@AttributeDefinition(name = "Service Ranking", description = "Service ranking; higher values are preferred.")
	int service_ranking() default 85;

	String webconsole_configurationFactory_nameHint() default "Tariff SMARD";
}