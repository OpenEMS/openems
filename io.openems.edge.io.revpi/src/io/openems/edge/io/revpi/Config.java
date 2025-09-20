package io.openems.edge.io.revpi;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "IO RevolutionPi DigitalIO Board", //
		description = "Implements the access to the Kunbus RevolutionPi DigitalIO enhancement hardware")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "io0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read DataOut Initially", description = "Init Outputs with state from hardware initially")
	boolean initOutputFromHardware() default true;

	String webconsole_configurationFactory_nameHint() default "IO RevolutionPi DigitalIO Board [{id}]";

}