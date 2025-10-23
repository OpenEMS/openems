package io.openems.edge.timeofusetariff.ancillarycosts;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Time-Of-Use Tariff Ancillary Costs", //
		description = "Time-Of-Use Tariff implementation for Ancillary Costs.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timeOfUseTariff0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Fixed Electricty tariff", description = "Gross price per kWh without ancillary costs [Cent/kWh]")
	double fixedTariff();

	@AttributeDefinition(name = "Ancillary Costs JSON", description = "Ancillary Costs in JSON format")
	String ancillaryCosts();

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff Ancillary Costs [{id}]";
}