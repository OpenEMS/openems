package io.openems.edge.bridge.modbus.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Modbus/TCP Bridge", //
		description = "Provides a service for connecting to, querying and writing to a Modbus/TCP device.")
@interface Config {
	String id();

	boolean enabled();

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the Modbus/TCP device.")
	String ip();

	String webconsole_configurationFactory_nameHint() default "Modbus/TCP Bridge [{id}]";
}