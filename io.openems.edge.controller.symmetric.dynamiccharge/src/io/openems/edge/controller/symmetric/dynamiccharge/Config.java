package io.openems.edge.controller.symmetric.dynamiccharge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Dynamic Charge Symmetric", //
		description = "Awattar Austria")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlDynamicCharge0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Predictions get-Hour", description = "Gets the predictions on this hour.")
	int getPredictionsHour() default 15;

	@AttributeDefinition(name = "Start-Hour", description = "Fallback start hour if no pv (Production more than Consumption)")
	int maxStartHour() default 8;

	@AttributeDefinition(name = "End-Hour", description = "fallback end hour if no pv. (Production less than Consumption)")
	int maxEndHour() default 17;

	@AttributeDefinition(name = "Hourly prices ", description = "The mock hourly-price values")
	String priceConfig() default "[{\n" + "	\"marketprice\": 61\n" + "}, {\n" + "	\"marketprice\": 62\n" + "}, {\n"
			+ "	\"marketprice\": 64\n" + "}, {\n" + "	\"marketprice\": 62\n" + "}, {\n" + "	\"marketprice\": 66\n"
			+ "}, {\n" + "	\"marketprice\": 62\n" + "}, {\n" + "	\"marketprice\": 68\n" + "}, {\n"
			+ "	\"marketprice\": 56\n" + "}, {\n" + "	\"marketprice\": 51\n" + "}, {\n" + "	\"marketprice\": 56.3\n"
			+ "}, {\n" + "	\"marketprice\": 58\n" + "}, {\n" + "	\"marketprice\": 61.2\n" + "}, {\n"
			+ "	\"marketprice\": 62\n" + "}, {\n" + "	\"marketprice\": 62\n" + "}]";

	String webconsole_configurationFactory_nameHint() default "Controller Dynamic Charge Symmetric [{id}]";

}