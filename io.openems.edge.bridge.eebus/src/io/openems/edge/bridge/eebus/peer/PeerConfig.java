package io.openems.edge.bridge.eebus.peer;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Bridge EEBUS Peer", description = "Provides a peer that communicates via eebus")
public @interface PeerConfig {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "eebuspeer0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "SKI", description = "EEBUS SKI of this peer")
	String ski();

	@AttributeDefinition(name = "EEBUS Bridge-ID", description = "Unique ID of EEBUS Bridge Component")
	String eebus_id() default "eebus0";

	String webconsole_configurationFactory_nameHint() default "Bridge EEBUS Peer [{id}]";

}
