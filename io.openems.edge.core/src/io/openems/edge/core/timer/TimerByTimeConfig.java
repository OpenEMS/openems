package io.openems.edge.core.timer;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@ObjectClassDefinition(//
        name = "Core Timer By Time", //
        description = "The global Timer By Time for TimerHandler.")
@interface TimerByTimeConfig {

    String webconsole_configurationFactory_nameHint() default "Core Timer By Time";

}