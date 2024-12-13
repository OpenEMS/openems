package io.openems.edge.timeofusetariff.rabotcharge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Time-Of-Use Tariff rabot.charge", //
		description = "Time-Of-Use Tariff implementation for rabot.charge.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timeOfUseTariff0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "ZIP Code", description = "German ZIP Code of the customer location")
	String zipcode();

	@AttributeDefinition(name = "Client Id", description = "Client Id of the client registration at rabot.charge")
	String clientId();

	@AttributeDefinition(name = "Client Secret", description = "Client Secret of the client registration at rabot.charge")
	String clientSecret();

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff rabot.charge [{id}]";
}
