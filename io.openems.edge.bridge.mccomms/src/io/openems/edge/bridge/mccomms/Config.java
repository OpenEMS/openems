package io.openems.edge.bridge.mccomms;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Bridge MC-Comms", //
		description = "Implements a generic bridge for the MCComms protocol.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "mccomms0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Serial port", description = "System serial port descriptor, eg. /dev/ttyUSB0, COM5")
	String serialPortDescriptor() default "COM5";

	@AttributeDefinition(name = "Packet window (ms)", description = "Number of milliseconds a received MCComms frame has to be completely read before being discarded as incomplete")
	int packetWindowMS() default 35;

	String webconsole_configurationFactory_nameHint() default "MCComms Bridge [{id}]";
}