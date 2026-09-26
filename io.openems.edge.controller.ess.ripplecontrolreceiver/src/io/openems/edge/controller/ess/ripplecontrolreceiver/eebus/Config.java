package io.openems.edge.controller.ess.ripplecontrolreceiver.eebus;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Ess Ripple Control Receiver, EEBUS", //
		description = "Controller to optimize energy distribution during peak hours by reducing the inverter output to 0, 30 or 60 percent.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "limiter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Eebus-ID", description = "ID of the EEBUS Bridge.")
	String eebus_id() default "eebus0";

	String webconsole_configurationFactory_nameHint() default "Controller Ess Ripple Control Receiver [{id}], EEEBUS";

}