package io.openems.edge.bridge.modbus;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Bridge Modbus/TCP", //
		description = "Provides a service for connecting to, querying and writing to a Modbus/TCP device.")
@interface ConfigTcp {
	String service_pid();

	String id() default "modbus0";

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the Modbus/TCP device.")
	String ip();

	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "Bridge Modbus/TCP [{id}]";
}