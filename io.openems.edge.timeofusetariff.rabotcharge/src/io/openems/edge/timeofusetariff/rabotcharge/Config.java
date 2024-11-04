package io.openems.edge.timeofusetariff.rabotcharge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
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
	
	@AttributeDefinition(name = "Access Token", description = "Access token for rabot.charge API", type = AttributeType.PASSWORD)
	String accessToken() default "";

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff rabot.charge [{id}]";
}
