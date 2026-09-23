package io.openems.edge.ess.saxpower.ess;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.type.Phase.SinglePhase;

@ObjectClassDefinition(//
		name = "SAX Power ESS", //
		description = "Implements the Sax Power ess system.")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ess0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId() default 100;

	@AttributeDefinition(name = "Phase", description = "Which Phase is this ESS connected to?")
	SinglePhase phase() default SinglePhase.L1;

	@AttributeDefinition(name = "Timeout", description = "Time in seconds after the battery switches to normal mode. Set value between 1 and 300")
	int timeout() default 60;

	String webconsole_configurationFactory_nameHint() default "SAX Power ESS [{id}]";
}