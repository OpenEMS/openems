package io.openems.edge.bosch.bpts5hybrid.core;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Bosch BPT-S 5 Core", //
		description = "Bosch BPT-S 5 Hybrid core component")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "boschBpts5hybridCore0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "IPv4 address of the Bosch BPT-S 5 Hybrid")
	String ipaddress() default "192.168.178.22";

	@AttributeDefinition(name = "Update Interval", description = "Update interval in seconds")
	int interval() default 2;

	String webconsole_configurationFactory_nameHint() default "Bosch BPT-S 5 Hybrid Core [{id}]";
}