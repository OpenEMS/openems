package io.openems.edge.controller.api.websocket;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Api Websocket", //
		description = "This controller provides an HTTP Websocket/JSON api. It is required for OpenEMS UI.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlApiWebsocket0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Port", description = "Port on which the Websocket server should listen.")
	int port() default 8085;

	@AttributeDefinition(name = "Api-Timeout", description = "Sets the timeout in seconds for updates on Channels set by this Api.")
	int apiTimeout() default 60;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	@AttributeDefinition(name = "External Authentication", description = "Allows authentication from trusted reverse proxy headers.")
	boolean externalAuthEnabled() default false;

	@AttributeDefinition(name = "External Authentication Trusted Proxy CIDRs", description = "IPv4 CIDR ranges that may provide external authentication headers, e.g. 127.0.0.1/32.")
	String[] externalAuthTrustedProxyCidrs() default {};

	@AttributeDefinition(name = "External Authentication User-ID Header", description = "HTTP header that contains the externally authenticated user id.")
	String externalAuthUserIdHeader() default "X-OpenEMS-User";

	@AttributeDefinition(name = "External Authentication User-Name Header", description = "HTTP header that contains the externally authenticated display name.")
	String externalAuthUserNameHeader() default "X-OpenEMS-Name";

	@AttributeDefinition(name = "External Authentication Role Header", description = "HTTP header that contains the OpenEMS role for the externally authenticated user.")
	String externalAuthRoleHeader() default "X-OpenEMS-Role";

	@AttributeDefinition(name = "External Authentication Default Role", description = "OpenEMS role for externally authenticated users if no role header is present.")
	String externalAuthDefaultRole() default "guest";

	String webconsole_configurationFactory_nameHint() default "Controller Api Websocket [{id}]";
}
