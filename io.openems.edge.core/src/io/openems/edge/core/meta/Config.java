package io.openems.edge.core.meta;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.currency.Currency;

@ObjectClassDefinition(//
		name = "Core Meta", //
		description = "The global manager for Metadata.")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Core Meta";

	@AttributeDefinition(name = "Currency", description = "Currency to be used for energy purchase; Energy price value is converted to appropraite currency based on current exchange rate")
	Currency currency() default Currency.EUR;

}