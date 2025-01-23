package io.openems.edge.timeofusetariff.awattar;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Time-Of-Use Tariff Awattar", //
		description = "Time-Of-Use Tariff implementation for aWATTar.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timeOfUseTariff0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Zone", description = "Zone corresponding to the customer's location")
	Zone zone() default Zone.GERMANY;

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff Awattar [{id}]";
}
