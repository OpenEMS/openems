package io.openems.edge.greenconsumptionadvisor.api;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Green Consumption Advice API", //
		description = "Provides a recomendation whether to use power from the grid or not based on current associated CO2 emissions per kWh.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "greenConsumptionAdvisor0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Zip-Code", description = "Zip-Code of the system location (Only works for Germany).")
	String zip_code();

	String webconsole_configurationFactory_nameHint() default "Green Consumption Advice API [{id}]";

}