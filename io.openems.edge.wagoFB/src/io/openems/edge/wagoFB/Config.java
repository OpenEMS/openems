package io.openems.edge.wagoFB;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "WAGO", //
		description = "Implements a WAGO.")
@interface Config {
	String service_pid();

	String id() default "wago0";

	@AttributeDefinition(name = "IP address", description = "IP address of the WAGO device")
	String ip();

	@AttributeDefinition(name = "Port", description = "The Port")
	int port() default 81;
	
	

	
	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "WAGO[{id}]";
}