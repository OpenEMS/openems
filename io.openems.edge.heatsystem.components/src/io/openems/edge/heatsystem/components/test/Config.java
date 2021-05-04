package io.openems.edge.heatsystem.components.test;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@ObjectClassDefinition(
        name = "Test HeatsystemComponent Valve/Pump",
        description = "A Component to Test a Valve or a Pump. Configure 2 of this if you want to test a Valve"
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Pump Name", description = "Unique Id of the Pump.")
    String id() default "HeatsystemComponentTest";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Test HeatsystemComponent [{id}]";
}
