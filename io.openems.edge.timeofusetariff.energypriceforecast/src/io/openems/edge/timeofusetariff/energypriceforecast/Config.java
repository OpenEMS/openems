package io.openems.edge.timeofusetariff.energypriceforecast;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Spot Tariff Energy Price Forecast EU", description = "Optional 48-hour wholesale price forecast. Keep published spot-price providers at a higher ranking.")
@interface Config {
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "tariffForecast0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Service Ranking", description = "Higher values are preferred. Keep this below providers of published prices.")
	int service_ranking() default -100;

	@AttributeDefinition(name = "Refresh interval", description = "Minutes between successful requests, per requested bidding zone.", min = "5", max = "1440")
	int refreshIntervalMinutes() default 30;

	@AttributeDefinition(name = "Maximum forecast age", description = "Maximum age in hours of the source forecast, including during API outages.", min = "1", max = "48")
	int maxForecastAgeHours() default 24;

	String webconsole_configurationFactory_nameHint() default "Spot Tariff Energy Price Forecast EU [{id}]";
}
