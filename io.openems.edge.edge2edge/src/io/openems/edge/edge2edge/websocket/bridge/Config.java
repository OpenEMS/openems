package io.openems.edge.edge2edge.websocket.bridge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Edge-2-Edge Bridge", //
		description = "Connects an energy storage system from a slave OpenEMS Edge via Modbus")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "bridge0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ip")
	String ip() default "127.0.0.1";

	@AttributeDefinition(name = "Port")
	String port() default "8085";

	@AttributeDefinition(name = "Remote Password", type = AttributeType.PASSWORD)
	String remotePassword();

	String webconsole_configurationFactory_nameHint() default "Edge-2-Edge Bridge [{id}]";

}