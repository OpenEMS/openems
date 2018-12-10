package com.ed.openems.centurio.edcom;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "KACO blueplanet 10.0 TL3 hybrid communication module", //
		description = "Implements the communication library for KACO blueplanet 10.0 TL3 hybrid system.")
@interface Config {
	String service_pid();

	String id() default "bpcom0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Serial Number", description = "The serial number  of the blueplanet 10.0 TL3 hybrid inverter.", required = false)
	String serialnumber();

	@AttributeDefinition(name = "IP", description = "The IP address of the blueplanet 10.0 TL3 hybrid inverter.", required = false)
	String ip();

	@AttributeDefinition(name = "Userkey", description = "The key / password for the blueplanet 10.0 TL3 hybrid inverter")
	String userkey();

	String webconsole_configurationFactory_nameHint() default "KACO blueplanet 10.0 TL3 hybrid bpCom [{id}]";
}
