package io.openems.edge.manager.valve;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Consolinno Manager Valve",
        description = "Manager for Valves.")

@interface Config {
    String service_pid();

    @AttributeDefinition(name = "Manager Valve - ID", description = "Id of Manager Valve.")
    String id() default "ValveManager";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "ValveManager [{id}]";
}