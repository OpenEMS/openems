package io.openems.edge.evcs.easee;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "EVCS easee", description = "Implements the easee Charging Station.")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the EVCS.")
    String id() default "evcs0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this EVCS.")
    String alias() default "";

    boolean enabled() default true;

    @AttributeDefinition(name = "Charging Priority", description = "Tick if the EVCS should charge with a higher priority.")
    boolean priority() default false;

    @AttributeDefinition(name = "Minimum power", description = "Minimum current of the Charger in A.")
    int minCurrent() default 8;

    @AttributeDefinition(name = "Maximum power", description = "Maximum current of the Charger in A.")
    int maxCurrent() default 32;

    @AttributeDefinition(name = "Phases", description = "If the Phases are physically swapped, change the order here.")
    int[] phases() default {1,2,3};

    @AttributeDefinition(name = "Username", description = "Username of the easee Account.")
    String username() default "";

    @AttributeDefinition(name = "Password", description = "Password of the easee Account", type = AttributeType.PASSWORD)
    String password() default "user";

    @AttributeDefinition(name = "chargerSerial", description = "ID (Serial) of the Charger.")
    String chargerSerial() default "";

    String webconsole_configurationFactory_nameHint() default "Easee Evcs [{id}]";
}
