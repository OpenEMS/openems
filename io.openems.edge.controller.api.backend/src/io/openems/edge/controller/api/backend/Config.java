package io.openems.edge.controller.api.backend;

import java.net.Proxy;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Api Backend", //
		description = "This controller connects to OpenEMS Backend")
@interface Config {
	String id() default "ctrlBackend0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Apikey", description = "Apikey for authentication at OpenEMS Backend.")
	String apikey();

	@AttributeDefinition(name = "Uri", description = "The connection Uri to OpenEMS Backend.")
	String uri() default "wss://fenecon.de:443/openems-backend2";

	@AttributeDefinition(name = "Cycle Time", description = "The time between sending data to Backend.")
	int cycleTime() default BackendApi.DEFAULT_CYCLE_TIME;

	@AttributeDefinition(name = "Proxy Address", description = "The IP address or hostname of the proxy server.")
	String proxyAddress() default "";

	@AttributeDefinition(name = "Proxy Port", description = "The port of the proxy server.")
	int proxyPort() default 0;

	@AttributeDefinition(name = "Proxy Type", description = "The type of the proxy server. (e.g. http)")
	Proxy.Type proxyType() default Proxy.Type.HTTP;

	@AttributeDefinition(name = "Api-Timeout", description = "Sets the timeout in seconds for updates on Channels set by this Api.")
	int apiTimeout() default 60;

	@AttributeDefinition(name = "Enable Debug mode")
	boolean debug() default false;

	String webconsole_configurationFactory_nameHint() default "Controller Api Backend [{id}]";
}