package io.openems.edge.timer.api;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "TimerByCycles", description = "This Timer counts Cycles. For each Component added to this Timer,"
        + " A Cycle Counter will start. When the Maximum Cycles are reached. The Components know, that the time is up.")
@interface TimerByCyclesConfig {

    @AttributeDefinition(name = "Id", description = "Unique Id for the TimerByCycles. You only need one though")
    String id() default "TimerByCycles";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "TimerByCycles {id}";
}
