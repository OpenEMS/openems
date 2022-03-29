package io.openems.edge.core.timer;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Core Timer By Counting", //
        description = "The global Timer By Counting for TimerHandler.")
@interface TimerByCountingConfig {

    String webconsole_configurationFactory_nameHint() default "Core Timer By Counting";

}