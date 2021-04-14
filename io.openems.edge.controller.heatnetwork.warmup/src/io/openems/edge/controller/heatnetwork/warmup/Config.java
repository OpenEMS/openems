package io.openems.edge.controller.heatnetwork.warmup;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Consolinno Warmup Program",
        description = "This Controller is used to run a warm up program."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the Passing Controller.")
    String id() default "ControllerWarmupPassing0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Controller.")
    String alias() default "WarmupPassing";

    @AttributeDefinition(name = "Auto Resume", description = "Automatically resumes the last running heating program if the controller was shut down while that program was still running (paused counts as running). Specifically, auto resume triggers when a config file is loaded that has \"elapsed time\">0. Turning this option off will set \"elapsed time\"=0 when the controller loads.")
    boolean auto_resume() default true;

    @AttributeDefinition(name = "Override Last Program", description = "The controller saves and loads the heating program of the previous run by default. This option overrides that and uses the following parameters instead. NOTE: \"elapsed time\" is not reset.")
    boolean override_program() default false;

    @AttributeDefinition(name = "Starting temperature", description = "The starting temperature of the heating run, in °C. Only used if override is enabled or there is no config file.")
    int start_temp() default 20;

    @AttributeDefinition(name = "Temperature increase per step", description = "The increase in temperature per heating step, in °C. Only used if override is enabled or there is no config file.")
    int temp_increase() default 5;

    @AttributeDefinition(name = "Number of steps", description = "The number of temperature steps, minimum 1. Only used if override is enabled or there is no config file.")
    int step_number() default 5;

    @AttributeDefinition(name = "Steps length", description = "The duration of each temperature step, in minutes. Only used if override is enabled or there is no config file.")
    int step_length() default 1;

    @AttributeDefinition(name = "Start on activation", description = "Immediately starts the heating program when the controller is loaded. Turning this off does NOT disable auto resume.")
    boolean start_on_activation() default false;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Consolinno WarmupPassing [{id}]";

}

