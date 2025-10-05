package io.openems.edge.timeofusetariff.manual.octopus.heat;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Time-Of-Use Tariff Octopus Heat", //
		description = "Time-Of-Use Tariff implementation for Octopus Heat")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timeofusetariff0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "Octopus Heat";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "High Price", description = "The high price, active from 06 to 09 pm [Cent/kWh]")
	double highPrice();

	@AttributeDefinition(name = "Standard Price", description = "The standard price [Cent/kWh]")
	double standardPrice();

	@AttributeDefinition(name = "Low Price", description = "The low price, active from 02 to 06 am and 12 am to 04 pm [Cent/kWh]")
	double lowPrice();

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff Octopus Heat [{id}]";

}
