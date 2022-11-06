package io.openems.edge.kaco.blueplanet.hybrid10.core;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "KACO blueplanet hybrid 10.0 TL3 Core", //
		description = "Implements the Core component for KACO blueplanet hybrid 10.0 TL3")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "kacoCore0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ident Key", description = "The proprietary ident key for the inverter as a hex string of the form '0xF0BA...'", type = AttributeType.PASSWORD)
	String identkey();

	@AttributeDefinition(name = "Serial Number", description = "The serial number of the inverter", required = false)
	String serialnumber();

	@AttributeDefinition(name = "IP", description = "The IP address of the inverter", required = false)
	String ip();

	@AttributeDefinition(name = "Userkey", description = "The key / password for the inverter", type = AttributeType.PASSWORD)
	String userkey();

	String webconsole_configurationFactory_nameHint() default "KACO blueplanet hybrid 10.0 TL3 Core [{id}]";
}
