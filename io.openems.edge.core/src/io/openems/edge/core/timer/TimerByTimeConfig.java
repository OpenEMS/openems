package io.openems.edge.core.timer;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "TimerByTime", description = "This Timer is like a Stopwatch, on initialization "
        + "it saves the Calling Time, whenever the corresponing OpenEMS component asks if the time is up, it checks "
        + "the initialize Time with the waitTime corresponding to the Component."
        + "The Time is in Seconds and the waitTime is configured individually by the OpenEMS Components themself.")
@interface TimerByTimeConfig {

    @AttributeDefinition(name = "Id", description = "Unique Id for the TimerByTime. You only need one though")
    String id() default "TimerByTime";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Timer by Time {id}";
}
