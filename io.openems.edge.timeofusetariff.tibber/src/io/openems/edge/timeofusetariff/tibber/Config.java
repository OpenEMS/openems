package io.openems.edge.timeofusetariff.tibber;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Time-Of-Use Tariff Tibber", //
		description = "Time-Of-Use Tariff implementation for Tibber.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timeOfUseTariff0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Access Token", description = "Access token for the Tibber API", type = AttributeType.PASSWORD)
	String accessToken() default "";

	@AttributeDefinition(name = "Filter for Home", description = "For multiple 'Homes', add either an ID (format UUID) or 'appNickname' for unambiguous identification", required = false)
	String filter() default "";

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff Tibber [{id}]";
}