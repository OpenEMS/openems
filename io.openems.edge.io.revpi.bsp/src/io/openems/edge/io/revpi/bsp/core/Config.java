package io.openems.edge.io.revpi.bsp.core;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
	name = "IO RevolutionPi BSP Core", //
	description = "Implements the Kunbus RevPi on board LEDs and the Relais")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "bsp0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "Kunbus BSP Core";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "Backend Component-ID", description = "Component-ID of the backend to surveillance and use for LED color")
    String backendComponentId() default "ctrlBackend0";

    String webconsole_configurationFactory_nameHint() default "IO RevolutionPi BSP Core[{id}]";
}