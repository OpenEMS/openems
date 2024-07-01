package io.openems.edge.controller.ess.timeframe;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Relationship;

@ObjectClassDefinition(//
        name = "Controller Ess Timeframe", //
        description = "Defines a timeframe to bring a symmetric energy storage system to a specified SoC.")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "ctrlTimeframe0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "Mode", description = "Set the type of mode.")
    Mode mode() default Mode.MANUAL;

    @AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
    String ess_id();

    @AttributeDefinition(name = "ESS Capacity", description = "Capacity of the ESS in Wh. Serves as Fallback, if ESS capacity can not be read from ESS.", required = false)
    int fallback_ess_capacity() default 0;

    @AttributeDefinition(name = "Target SoC [%]", description = "SoC to reach within the specified timeframe")
    int targetSoC();

    @AttributeDefinition(name = "Start Time", description = "Start of the timeframe used in manual mode; ISO 8601 format")
    String startTime();

    @AttributeDefinition(name = "End Time", description = "End of the timeframe used in manual mode; ISO 8601 format")
    String endTime();

    @AttributeDefinition(name = "Power Relationship", description = "Target power must be equal, less-than or greater-than the configured power value")
    Relationship relationship() default Relationship.EQUALS;

    @AttributeDefinition(name = "Phase", description = "Apply target power to L1, L2, L3 or sum of all phases")
    Phase phase() default Phase.ALL;

    String webconsole_configurationFactory_nameHint() default "Controller Ess Timeframe [{id}]";
}