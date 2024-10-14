package io.openems.edge.core.timer;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Core TimerManager", //
        description = "Global Access to different kind of timers.")
@interface Config {

    String webconsole_configurationFactory_nameHint() default "Core TimerManager";

}
