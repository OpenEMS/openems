package io.openems.edge.timeofusetariff.hassfurt;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Time-Of-Use Tariff Stadwerke Hassfurt", //
		description = "Time-Of-Use Tariff implementation for Stadwerke Hassfurt.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timeOfUseTariff0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Tariff Type", description = "Tariff type that the customer has subscribed to")
	TariffType tariffType() default TariffType.STROM_FLEX;

	String webconsole_configurationFactory_nameHint() default "Time-Of-Use Tariff Stadwerke Hassfurt [{id}]";
}
