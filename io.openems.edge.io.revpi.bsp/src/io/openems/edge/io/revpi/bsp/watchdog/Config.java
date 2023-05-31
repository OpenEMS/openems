package io.openems.edge.io.revpi.bsp.watchdog;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "IO RevolutionPi BSP Watchdog", //
	description = "Implements the Kunbus RevPi on board LEDs and the Relais")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "watchdog0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "Kunbus BSP Watchdog";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "IO RevolutionPi BSP Watchdog[{id}]";
}
