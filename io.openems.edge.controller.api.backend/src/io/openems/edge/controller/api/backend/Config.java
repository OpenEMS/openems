package io.openems.edge.controller.api.backend;

import java.net.Proxy;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Api Backend", //
		description = "This controller connects to OpenEMS Backend")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlBackend0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Apikey", description = "Apikey for authentication at OpenEMS Backend.", type = AttributeType.PASSWORD)
	String apikey();

	@AttributeDefinition(name = "Uri", description = "The connection Uri to OpenEMS Backend.")
	String uri() default "wss://www1.fenecon.de:443/openems-backend2";

	@AttributeDefinition(name = "No. of Cycles", description = "How many Cycles till data is sent to OpenEMS Backend.")
	int noOfCycles() default BackendApiImpl.DEFAULT_NO_OF_CYCLES;

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