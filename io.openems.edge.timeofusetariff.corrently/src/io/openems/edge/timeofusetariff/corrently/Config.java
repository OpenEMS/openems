package io.openems.edge.timeofusetariff.corrently;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Time-Of-Use Tariff Corrently", //
		description = "Time-Of-Use Tariff implementation for Corrently.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timeOfUseTariff0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "ZIP Code", description = "German ZIP Code of the customer location")
	String zipcode();

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff Corrently [{id}]";
}