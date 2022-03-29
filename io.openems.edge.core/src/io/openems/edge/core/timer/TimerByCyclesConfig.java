package io.openems.edge.core.timer;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Core Timer By Cycles", //
        description = "The global Timer By Cycles for TimerHandler.")
@interface TimerByCyclesConfig {

    String webconsole_configurationFactory_nameHint() default "Core Timer By Cycles";

}