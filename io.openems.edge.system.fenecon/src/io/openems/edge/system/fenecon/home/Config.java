package io.openems.edge.system.fenecon.home;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.system.fenecon.home.enums.LedOrder;

@ObjectClassDefinition(//
		name = "System FENECON Home", //
		description = "Implements the FENECON Home core component")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "system0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Id of the Relay", description = "Id of the Relay. (Default: should be the internal GPIO)")
	String relayId() default "io1";

	@AttributeDefinition(name = "LED order", description = "The LED order according to their relay contact")
	LedOrder ledOrder() default LedOrder.DEFAULT_RED_BLUE_GREEN;

}
