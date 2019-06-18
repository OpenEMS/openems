package io.openems.edge.bridge.mbus;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.AttributeDefinition;

@ObjectClassDefinition( //
		name = "Bridge M-Bus", //
		description = "Provides a service for connecting to, reading and writing an M-Bus device.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "mbus0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Serial-Device", description = "Serial Device Name")
	String device() default "/dev/ttyUSB0";

	@AttributeDefinition(name = "Serial-Speed", description = "Serial Device Speed")
	int baud() default 2400;

	String webconsole_configurationFactory_nameHint() default "Bridge M-Bus [{id}]";
}