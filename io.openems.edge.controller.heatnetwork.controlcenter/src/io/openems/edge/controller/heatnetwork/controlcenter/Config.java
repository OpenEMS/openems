package io.openems.edge.controller.heatnetwork.controlcenter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Consolinno Passing Control Center",
        description = "Control module of the passing station."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the Passing Controller.")
    String id() default "ControllerPassingControlCenter0";

    @AttributeDefinition(name = "alias", description = "Human Readable Name of Component.")
    String alias() default "Uebergabestation Control Center";

    @AttributeDefinition(name = "Allocated Pid Controller", description = "Unique Name of the Pid Controller, allocated with this Control Center.")
    String allocated_Pid_Controller() default "PidController0";

    @AttributeDefinition(name = "Allocated Pump", description = "Pump to activate")
    String allocated_Pump() default "RelayPump";

    @AttributeDefinition(name = "Allocated Warmup Controller", description = "Unique Name of the Warmup Controller, allocated with this Control Center.")
    String allocated_Warmup_Controller() default "ControllerWarmupPassing0";

    @AttributeDefinition(name = "Run Warmup Program", description = "Run the program loaded in the Warmup Controller.")
    boolean run_warmup_program() default false;

    @AttributeDefinition(name = "Allocated Heating Curve Regulator", description = "Unique name of the automatic regulator that adjusts heating depending on outside temperature.")
    String allocated_Heating_Curve_Regulator() default "HeatingCurveRegulator0";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Consolinno Passing Control Center [{id}]";
}