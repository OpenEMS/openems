package io.openems.edge.sonnen.core;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Sonnen Core", //
		description = "sonnenBatterie core component. Connects to the local JSON-API of the sonnenBatterie.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "sonnenCore0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "IPv4 address of the sonnenBatterie")
	String ipAddress() default "192.168.178.35";

	@AttributeDefinition(name = "Port", description = "Port of the sonnenBatterie local JSON-API")
	int port() default 80;

	@AttributeDefinition(name = "API Token", description = "Authentication token of the sonnenBatterie local JSON-API "
			+ "(Webinterface: 'Software-Integration'). Required for the Write-API and some firmwares also for "
			+ "the Powermeter endpoint.")
	String apiToken() default "";

	@AttributeDefinition(name = "Read Interval", description = "Read interval in seconds")
	int readInterval() default 5;

	String webconsole_configurationFactory_nameHint() default "Sonnen Core [{id}]";
}
