package io.openems.edge.controller.heatnetwork.pid.heatsystem;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Consolinno Pid For Passing",
        description = "This Controller regulates the Pump and Valves for Heating via Pid Regulation."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the Passing Controller.")
    String id() default "PidController0";

    @AttributeDefinition(name = "alias", description = "Human Readable Name of Component.")
    String alias() default "";

    @AttributeDefinition(name = "Use Dependency", description = "PID Reacting only to active Passing station")
    boolean useDependency() default false;

    @AttributeDefinition(name = "Dependency : what component can block this Controller", description = "Component that can block this component")
    String passingControllerId() default "PassingController0";

    @AttributeDefinition(name = "Allocated Passing Device", description = "Unique Name of the allocated Device.")
    String allocatedPassingDevice() default "Valve0";

    @AttributeDefinition(name = "Proportional Gain", description = "The proportional gain value for PID.")
    double proportionalGain () default 2.0;

    @AttributeDefinition(name = "Integral Gain", description = "The integral gain value for PID.")
    double integralGain () default 0.2;

    @AttributeDefinition(name = "Derivative Gain", description = "The derivative gain value for PID.")
    double derivativeGain () default 0.1;

    @AttributeDefinition(name = "Temperature", description = "The Temperature you want to reach (T in dC--> 1°C = 10).")
    int setPoint_Temperature() default 750;

    @AttributeDefinition(name = "Turn off Percentage", description = "If PID is off position of controlled device")
    int offPercentage() default 0;

    @AttributeDefinition(name = "TemperatureSensor", description = "The Temperaturesensor allocated to this controller")
    String temperatureSensorId() default "TemperatureSensor4";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller PID For Passing [{id}]";
}