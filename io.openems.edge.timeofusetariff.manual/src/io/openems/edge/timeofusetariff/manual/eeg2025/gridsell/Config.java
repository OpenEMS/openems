package io.openems.edge.timeofusetariff.manual.eeg2025.gridsell;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.EntsoeBiddingZone;

@ObjectClassDefinition(//
		name = "Tariff Manual EEG 2025 Grid-Sell", //
		description = "Implementation of a grid-sell tariff according to EEG 2025, with a fixed grid-sell price that is set to zero if the day-ahead market price is negative.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "tariffGridSell0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Fixed Grid-Sell Price", description = "Fixed price for electricity sold to the grid")
	double fixedGridSellPrice() default 0.0;

	@AttributeDefinition(name = "Security Token", description = "Security token for the ENTSO-E Transparency Platform", type = AttributeType.PASSWORD, required = false)
	String securityToken() default "";

	@AttributeDefinition(name = "Bidding Zone", description = "Zone corresponding to the location")
	EntsoeBiddingZone biddingZone();

	String webconsole_configurationFactory_nameHint() default "Tariff Manual EEG 2025 Grid-Sell [{id}]";
}