package io.openems.edge.edge2edge.websocket.dccharger;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Edge-2-Edge DC Charger Websocket", //
		description = "Connects a DC charger from a slave OpenEMS Edge via Websocket")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "charger0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Remote Component-ID", description = "Component-ID of the remote DC charger at the slave OpenEMS Edge.")
	String remoteComponentId() default "charger0";

	@AttributeDefinition(name = "Bridge-ID")
	String bridge_id() default "bridge0";

	String webconsole_configurationFactory_nameHint() default "Edge-2-Edge DC Charger Websocket [{id}]";

}
