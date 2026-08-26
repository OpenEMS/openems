package io.openems.edge.timeofusetariff.manual.fixed.gridsell;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Tariff Manual Fixed Grid-Sell", //
		description = "Implementation of a grid-sell tariff with a fixed grid-sell price.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "tariffGridSell0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Fixed Grid-Sell Price", description = "Fixed price for electricity sold to the grid")
	double fixedGridSellPrice() default 0.0;

	String webconsole_configurationFactory_nameHint() default "Tariff Manual Fixed Grid-Sell [{id}]";
}