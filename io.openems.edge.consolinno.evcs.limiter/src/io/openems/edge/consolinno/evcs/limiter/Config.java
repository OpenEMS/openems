package io.openems.edge.consolinno.evcs.limiter;

import io.openems.edge.evcs.api.GridVoltage;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Evcs Limiter", description = "Limit the power of the EVCS connected in a cluster.")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the Limiter.")
    String id() default "ConsolinnoLimiter";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Limiter.")
    String alias() default "";

    boolean enabled() default true;

    @AttributeDefinition(name = "GridVoltage", description = "Voltage of the Power Grid.")
    GridVoltage grid() default GridVoltage.V_230_HZ_50;

    @AttributeDefinition(name = "evcsIDs", description = "Ids of the EVCS that have to be managed.")
    String[] evcss() default {"evcs0","evcs1"};

    @AttributeDefinition(name = "useMeter", description = "Tick if the controller should offset its calculation based on an existing Meter.")
    boolean useMeter() default true;

    @AttributeDefinition(name = "MeterID", description = "Id of the Meter used for Calculation of the available power.")
    String meter() default "";

    @AttributeDefinition(name = "symmetry", description = "Check if the EVCS should stay balanced in their load.")
    boolean symmetry() default true;

    @AttributeDefinition(name = "symmetryOffset", description = "Number of Cycles how long the Limiter should wait before restoring symmetry.")
    int symmetryOffset() default 0;

    @AttributeDefinition(name = "SymmetryMeterID", description = "Id of the Meter used for symmetry.")
    String symmetryMeter() default "";

    @AttributeDefinition(name = "symmetryDelta", description = "Maximum difference between two Phases.")
    int symmetryDelta() default 20;

    @AttributeDefinition(name = "offTime", description = "Time (in minutes) how long an EVCS should be turned off if the Power has to be balanced.")
    int offTime() default 0;

    @AttributeDefinition(name = "phaseLimit", description = "Maximum Current (A) one Phase can pull from the grid.")
    int phaseLimit() default 0;

    @AttributeDefinition(name = "powerLimit", description = "Maximum Power (W) the entire EVCS cluster can pull from the grid.")
    int powerLimit() default 0;

    @AttributeDefinition(name = "priorityCurrent", description = "Current (A) a high priority EVCS should at least charge with,"
            + "or share with other high priority EVCS if there is not enough current present.")
    int priorityCurrent() default 32;

    @AttributeDefinition(name = "minimumAssign", description = "Minimum current that has to be given to a Evcs before the Limiter assigns new Power.")
    int minimumAssign() default 1;

    @AttributeDefinition(name = "deltaTime", description = "Time (in Minutes) between assignment of new (higher) Power.")
    int deltaTime() default 0;

    @AttributeDefinition(name = "phaseTolerance", description = "Amount of Amperage that is allowed beyond the Phaselimit before the Limiter reduces the current.")
    int phaseTolerance() default 0;

    @AttributeDefinition(name = "powerTolerance", description = "Amount of Amperage that is allowed beyond the Powerlimit before the Limiter reduces the current.")
    int powerTolerance() default 0;

    String webconsole_configurationFactory_nameHint() default "Consolinno EvcsLimiter [{id}]";
}
