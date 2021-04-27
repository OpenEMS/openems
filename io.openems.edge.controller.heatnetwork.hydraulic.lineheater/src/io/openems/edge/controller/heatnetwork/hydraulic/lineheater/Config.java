package io.openems.edge.controller.heatnetwork.hydraulic.lineheater;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Controller Consolinno Hydraulic Line Heater",
        description = "This Controller heats ab a hydraulic line."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the Controller.")
    String id() default "HydraulicLineHeater0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Controller.")
    String alias() default "Hydraulic-Line";


    @AttributeDefinition(name = "Should Fallback be activated", description = "if there is no signal, hold the line on temperatrue")
    boolean shouldFallback() default false;

    @AttributeDefinition(name = "Reference Temperature", description = "The Temperature-Sensor for the LineHeater.")
    String tempSensorReference() default "TemperatureSensor0";

    @AttributeDefinition(name = "Default Temperature", description = "How long should we heat? OFF(in dC --> 1°C == 10°dC).")
    int temperatureDefault() default 800;

    @AttributeDefinition(name = "Valve or Channel", description = "Do you want to Access a Channeldirectly or a Valve",
            options = {
                    @Option(label = "Channel", value = "Channel"),
                    @Option(label = "Valve", value = "Valve")})
    String valveOrChannel() default "Valve";

    @AttributeDefinition(name = "Value to Write is Boolean", description = "Is the Value you want to write a Boolean "
            + "otherwise the component calculates the amount of % to open the Valve")
    boolean valueToWriteIsBoolean() default false;

    @AttributeDefinition(name = "Channels To Read and Write From", description = "First Channel is to Read, Second to Write, only important if Channel controlled")
    String[] channels() default {"valve0/PowerLevel", "valve0/SetPowerLevel"};

    @AttributeDefinition(name = "Reference Valve", description = "The Valve for the LineHeater.")
    String valveBypass() default "Valve0";


    @AttributeDefinition(name = "Timeout of Remote signal", description = "Seconds after no signal that the fallack starts")
    int timeoutMaxRemote() default 30;

    @AttributeDefinition(name = "Restart cycle after time", description = "if the sensor gets cold again and a new cycle should be startet")
    int timeoutRestartCycle() default 600;

    @AttributeDefinition(name = "Minute for fallbackstart", description = "always start at X:30")
    int minuteFallbackStart() default 0;

    @AttributeDefinition(name = "Minute for fallbackstop", description = "always stop at X:45")
    int minuteFallbackStop() default 30;

    boolean useDecentralHeater() default false;

    @AttributeDefinition(name = "optional Decentralheater", description = "If a Decentralheater directly needs a communication")
    String decentralheaterReference() default "Decentralheater0";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Hydraulic Line Heater [{id}]";

}