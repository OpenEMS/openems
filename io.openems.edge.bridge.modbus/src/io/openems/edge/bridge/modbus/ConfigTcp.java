package io.openems.edge.bridge.modbus;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.ghgande.j2mod.modbus.Modbus;

import io.openems.edge.bridge.modbus.api.LogVerbosity;

@ObjectClassDefinition(//
		name = "Bridge Modbus/TCP", //
		description = "Provides a service for connecting to, querying and writing to a Modbus/TCP device.")
@interface ConfigTcp {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "modbus0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the Modbus/TCP device.")
	String ip();

	@AttributeDefinition(name = "Port", description = "The port of the Modbus/TCP device.")
	int port() default Modbus.DEFAULT_PORT;

	@AttributeDefinition(name = "Log-Verbosity", description = "The log verbosity.")
	LogVerbosity logVerbosity() default LogVerbosity.NONE;

	@AttributeDefinition(name = "Invalidate elements after how many read Errors?", description = "Increase this value if modbus read errors happen frequently.")
	int invalidateElementsAfterReadErrors() default 1;

	String webconsole_configurationFactory_nameHint() default "Bridge Modbus/TCP [{id}]";
}