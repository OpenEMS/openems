package io.openems.edge.controller.symmetric.randompower;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Random Power Symmetric", //
		description = "Defines a random power within fixed max/min power to a symmetric energy storage system.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlRandomPower0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Min power [W]", description = "Negative values for Charge; positive for Discharge")
	int minPower();

	@AttributeDefinition(name = "Max power [W]", description = "Negative values for Charge; positive for Discharge")
	int maxPower();

	String webconsole_configurationFactory_nameHint() default "Controller Random Power Symmetric [{id}]";
}