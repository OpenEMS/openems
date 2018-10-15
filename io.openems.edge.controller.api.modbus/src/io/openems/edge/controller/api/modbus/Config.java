package io.openems.edge.controller.api.modbus;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Api Modbus/TCP", //
		description = "This controller provides a Modbus/TCP api.")
@interface Config {
	String service_pid();

	String id() default "ctrlApiModbus0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Port", description = "Port on which the server should listen.")
	int port() default 502;

	@AttributeDefinition(name = "Components", description = "Components that should be made available via Modbus.")
	String[] component_ids() default { "_sum" };

	@AttributeDefinition(name = "Api-Timeout", description = "Sets the timeout in seconds for updates on Channels set by this Api.")
	int apiTimeout() default 60;

	String webconsole_configurationFactory_nameHint() default "Controller Api Modbus/TCP [{id}]";
}