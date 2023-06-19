package io.openems.edge.bridge.http;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Bridge HTTP", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "http0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Verbose mode enabled?", description = "If enabled, print all requests and respones.")
	boolean verbose() default false;
	
	@AttributeDefinition(name = "Test Connection", description = "If enabled, the component tries to make a HTTP GET request to https://fenecon.de/ each cycle.")
	boolean test_connection() default false;

	String webconsole_configurationFactory_nameHint() default "Bridge.Http [{id}]";

}