package io.openems.edge.bridge.eebus;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Bridge EEBUS", description = "Provides a service for connecting to EEBUS devices.")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "eebus0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Binding Host", description = "Hostname on which the eebus server should listen.")
	String bindHost() default "";

	@AttributeDefinition(name = "Binding Port", description = "Port on which the Websocket server should listen.")
	int bindPort() default 8085;

	@AttributeDefinition(name = "Service ID", description = "EEBUS Service ID. Internal name used for multicast. Should be unique and only contain characters [0-9A-Z-]")
	String serviceID();

	@AttributeDefinition(name = "Service Instance", description = "Friendly name for this EEBUS service. Visible to other EEBUS devices. Supports UTF-8, spaces, and up to 63 characters.")
	String serviceInstance();

	@AttributeDefinition(name = "TLS Certificate", description = "Self-signed TLS certificate for EEBUS. Automatically generated if left empty. Changing this value will also change the SKI.")
	String tlsCertificate();

	@AttributeDefinition(name = "Debug Mode", description = "Enable debug logging for MQTT communication")
	boolean debugMode() default false;

	String webconsole_configurationFactory_nameHint() default "Bridge EEBUS [{id}]";
}
