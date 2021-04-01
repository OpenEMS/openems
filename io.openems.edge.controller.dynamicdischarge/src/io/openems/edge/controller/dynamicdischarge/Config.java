package io.openems.edge.controller.dynamicdischarge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Dynamic Discharge", //
		description = "controller schedules the battery based on dynamic prices in germany market base")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlDynamicDischarge0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "url", description = "URL to connect API", type = AttributeType.STRING)
	String url() default "https://api.awattar.com/v1/marketdata";

	@AttributeDefinition(name = "Apikey", description = "Apikey for authenticating Awattar API.", type = AttributeType.PASSWORD)
	String apikey() default "ak_7YTR42jBwtnk5kXuMZRYEju8hvj918H0";

	@AttributeDefinition(name = "Predictions start-Hour", description = "Gets the predictions on this hour.")
	int predictionStartHour() default 15;

	@AttributeDefinition(name = "Morning-Hour", description = "Calculation stop at this hour")
	int maxStartHour() default 7;

	@AttributeDefinition(name = "Evening-Hour", description = "Calculation starts at this hour")
	int maxEndHour() default 16;

	@AttributeDefinition(name = "Hourly prices ", description = "The mock hourly-price values, used only for Junit test case")
	String priceConfig() default "[{\n" + "	\"marketprice\": 61\n" + "}, {\n" + "	\"marketprice\": 62\n" + "}]";

	String webconsole_configurationFactory_nameHint() default "Controller Dynamic Discharge [{id}]";

}
