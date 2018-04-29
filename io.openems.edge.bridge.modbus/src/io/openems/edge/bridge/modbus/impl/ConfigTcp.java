package io.openems.edge.bridge.modbus.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Modbus/TCP Bridge", //
		description = "Provides a service for connecting to, querying and writing to a Modbus/TCP device.")
@interface ConfigTcp {
	String service_pid();

	String id();

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the Modbus/TCP device.")
	String ip();

	boolean enabled();

	String webconsole_configurationFactory_nameHint() default "Modbus/TCP Bridge [{id}]";
}