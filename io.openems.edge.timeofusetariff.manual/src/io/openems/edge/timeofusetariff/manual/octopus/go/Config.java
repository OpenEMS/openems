package io.openems.edge.timeofusetariff.manual.octopus.go;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Time-Of-Use Tariff Octopus Go", //
		description = "Time-Of-Use Tariff implementation for Octopus Go")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timeofusetariff0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "Octopus Go";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Standard Price", description = "The standard price [€/kWh]")
	double standardPrice();

	@AttributeDefinition(name = "Low Price", description = "The low price, active between 00 and 05 am [€/kWh]")
	double lowPrice();

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff Octopus Go [{id}]";

}