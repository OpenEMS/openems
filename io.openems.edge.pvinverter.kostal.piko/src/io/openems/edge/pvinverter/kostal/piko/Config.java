package io.openems.edge.pvinverter.kostal.piko;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
		name = "PV-Inverter KOSTAL PIKO", //
		description = "Implements the KOSTAL PIKO PV inverter via HTTP/HTML parsing.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "pvinverter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the PIKO inverter")
	String ip();

	@AttributeDefinition(name = "Username", description = "Username for Basic Authentication")
	String username() default "pvserver";

	@AttributeDefinition(name = "Password", description = "Password for Basic Authentication")
	String password() default "pvwr";

	String webconsole_configurationFactory_nameHint() default "PV-Inverter KOSTAL PIKO [{id}]";
}