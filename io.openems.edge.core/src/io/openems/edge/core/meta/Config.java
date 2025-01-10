package io.openems.edge.core.meta;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.CurrencyConfig;

@ObjectClassDefinition(//
		name = "Core Meta", //
		description = "The global manager for Metadata.")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Core Meta";

	@AttributeDefinition(name = "Currency", description = "Every monetary value is inherently expressed in this Currency. Values obtained in a different currency (e.g. energy prices from a web service) are internally converted to this Currency using the current exchange rate.")
	CurrencyConfig currency() default CurrencyConfig.EUR;

	@AttributeDefinition(name = "Is Ess Charge From Grid Allowed", description = "Charging the battery from grid is allowed.")
	boolean isEssChargeFromGridAllowed() default false;

}