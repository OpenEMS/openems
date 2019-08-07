package io.openems.edge.bridge.mccomms;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( 
		name = "MCComms Bridge", //
		description = "Implements a generic bridge for the MCComms protocol.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "mccomms0"; 

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default ""; 

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true; 

	@AttributeDefinition(name = "MCComms-ID", description = "MCComms ID of this bridge")
	int mccomms_id(); 

	String webconsole_configurationFactory_nameHint() default "MCComms Bridge [{id}]"; 
}