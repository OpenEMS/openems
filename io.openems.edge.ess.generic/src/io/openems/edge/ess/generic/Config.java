package io.openems.edge.ess.generic;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "ESS Generic", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "io.openems.edge.ess.generic0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Battery-Inverter-ID", description = "ID of Battery-Inverter.")
	String inverter_id() default "batteryInverter0";

	@AttributeDefinition(name = "Battery-ID", description = "ID of Battery.")
	String battery_id() default "bms0";

	String webconsole_configurationFactory_nameHint() default "ESS Generic [{id}]";

}