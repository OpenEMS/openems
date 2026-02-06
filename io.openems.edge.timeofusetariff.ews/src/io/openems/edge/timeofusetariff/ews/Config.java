package io.openems.edge.timeofusetariff.ews;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Time-Of-Use Tariff EWS Schönau", //
		description = "Time-Of-Use Tariff implementation for EWS Schönau.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timeOfUseTariff0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Access Token", description = "Access token for the EWS API (ask api@ews-schoenau.de for a key)", type = AttributeType.PASSWORD)
	String accessToken() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff Ews [{id}]";
}