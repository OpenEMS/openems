package com.ed.openems.centurio.edcom;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Energy Depot EdCom", //
		description = "Implements the Energy Depot EdCom library for communication with the Centurio system.")
@interface Config {
	String service_pid();

	String id() default "edcom0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Serial Number", description = "The serial number  of the Centurio inverter.", required = false)
	String serialnumber();

	@AttributeDefinition(name = "IP", description = "The IP address of the Centurio inverter.", required = false)
	String ip();

	@AttributeDefinition(name = "Userkey", description = "The key / password for the Centurio inverter")
	String userkey();

	String webconsole_configurationFactory_nameHint() default "Energy Depot EdCom [{id}]";
}
