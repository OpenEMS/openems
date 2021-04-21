package io.openems.edge.remote.rest.device.simulator;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@ObjectClassDefinition(
        name = "Test Rest Remote Device",
        description = " This Simulator is for testing the Rest Remote Device Read and Write Ability. WriteValues will be set to Read Values etc. ")
@interface Config {


    String service_pid();

    @AttributeDefinition(name = "Unique Id of Device", description = "Id of the Device")
    String id() default "TestDevice0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Rest Device [{id}]";

}