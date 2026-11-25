package io.openems.edge.edge2edge.websocket.ess.hybrid;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.channel.AccessMode;

@ObjectClassDefinition(//
		name = "Edge-2-Edge Hybrid ESS Websocket", //
		description = "Connects a hybrid energy storage system from a slave OpenEMS Edge via Websocket, with PV-Charger support")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ess0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Remote Access-Mode", description = "Access-Mode to the Component at the slave OpenEMS Edge?")
	AccessMode remoteAccessMode() default AccessMode.READ_ONLY;

	@AttributeDefinition(name = "Remote Component-ID", description = "Component-ID of Hybrid ESS at the slave OpenEMS Edge.")
	String remoteComponentId() default "ess0";

	@AttributeDefinition(name = "DC Charger Component-IDs", description = "Local Edge-2-Edge Websocket DC charger components used to derive PV production and surplus power.")
	String[] dcChargerIds() default {};

	@AttributeDefinition(name = "Bridge-ID")
	String bridge_id() default "bridge0";

	String webconsole_configurationFactory_nameHint() default "Edge-2-Edge Hybrid ESS Websocket [{id}]";

}
