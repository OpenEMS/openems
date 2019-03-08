package io.openems.edge.evcs.keba.kecontact;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "EVCS KEBA KeContact", //
		description = "Implements the KEBA KeContact P20/P30 electric vehicle charging station.")
@interface Config {
	String id() default "evcs0";

	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the charging station.")
	String ip();

	String webconsole_configurationFactory_nameHint() default "EVCS KEBA KeContact [{id}]";
}