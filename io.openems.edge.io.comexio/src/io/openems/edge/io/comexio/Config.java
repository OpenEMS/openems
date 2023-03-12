package io.openems.edge.io.comexio;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.MeterType;

@ObjectClassDefinition(//
		name = "IO Generic HTTP Comexio Implementation", //
		description = "Implements a Generic HTTP IO for Comexio Smarthome")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "io0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP", description = "IP of the Comexio-Smarthome-Server")
	String ip();
	
	@AttributeDefinition(name = "Group Prefix", description = "Name of the Server or extension of Comexio-Smarthome e.g. \"IO-Server\"")
	String group_prefix() default "IO-Server";
	
	@AttributeDefinition(name = "Username", description = "If the API is protected by a login please type in the API-Username if not leave it blank")
	String username() default "";
	
	@AttributeDefinition(name = "Password", description = "If the API is protected by a login please type in the API-Password if not leave it blank")
	String password() default "";
	
	@AttributeDefinition(name = "Voltage Level", description = "Comexio only proviedes a Current-Value per Relay. To calculate the Power we need the Voltage-Level applied")
	String voltage() default "230.0";
	
	@AttributeDefinition(name = "Timeout in ms", description = "Please define a timeout in ms for the HTTP-Requests")
	int timeout() default 5000;	
	
	@AttributeDefinition(name = "Meter-Type", description = "What is measured by this Meter?")
	MeterType type() default MeterType.CONSUMPTION_METERED;

	String webconsole_configurationFactory_nameHint() default "IO Generic HTTP Comexio Implementation [{id}]";
}