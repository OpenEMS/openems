package io.openems.edge.ess.kostal.piko;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "ESS KOSTAL PIKO", //
		description = "Implements a KOSTAL PIKO based energy storage system.")
@interface Config {
	String service_pid();

	String id() default "ess0";

	@AttributeDefinition(name = "IP-Address", description = "The IP address")
	String ip();

	@AttributeDefinition(name = "Unit ID", description = "The Unit ID")
	int unitID() default 0xff;

	@AttributeDefinition(name = "Port", description = "The Port")
	int port() default 81;
	
	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "ESS KOSTAL PIKO [{id}]";
}