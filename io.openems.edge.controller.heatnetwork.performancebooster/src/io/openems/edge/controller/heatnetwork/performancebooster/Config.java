package io.openems.edge.controller.heatnetwork.performancebooster;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Heatnetwork PerformanceBooster",
        description = "A Controller controlling a heat mixer depending on certain Heaters."
)
@interface Config {
    String service_pid();

    @AttributeDefinition(name = "HeatnetworkPerformanceBooster-ID", description = "ID of Heatnetwork PerformanceBooster.")
    String id() default "HeatNetworkBooster0";

    @AttributeDefinition(name = "Alias", description = "Human readable Name.")
    String alias() default "GLT-Booster-Controller";

    @AttributeDefinition(name = "Control center ", description = "waits for activation")
    String allocatedControlCenter() default "ControlCenter0";


    @AttributeDefinition(name = "DelayTime", description = "wait seconds after activation switches")
    int waitingAfterActive() default 300;


    @AttributeDefinition(name = "MaxTemperatureSetPoint", description = "Max Temperature --> max in dC! for Buffer")
    int maxTemp() default 500;

    @AttributeDefinition(name = "MaxLiter in Buffer", description = "Max Litres in Buffer possible")
    int litres() default 6000;

    @AttributeDefinition(name = "Max Buffer Threshold", description = "Max Percentage Buffer Threshold in %")
    int maxBufferThreshold() default 120;

    @AttributeDefinition(name = "Heat Mixer SetPoint", description = "Percentage Value of Valve when Controller activates.")
    int valvePercent() default 48;

    @AttributeDefinition(name = "Heat Mixer addition", description = "Percentage increase if Error occurred in primary Heater ")
    int valvePercentAdditional() default 20;

    @AttributeDefinition(name = "Percent Increase Performance Heater 2 Error", description = "If Secondary Heater got a Error this Percent will In/Decrease (negative Numbers if it should decrease).")
    int valvePercentSubtraction() default -10;

    @AttributeDefinition(name = "Heater Backup Performance", description = "If the Temperature drops, the backup heater will be set to this %.")
    int backUpPercent() default 30;

    @AttributeDefinition(name = "Percent Increase Performance", description = "Additional Percentage Increase if Error occurred in Primary Heater.")
    int backUpPercentAdditionalHeater1Error() default 20;


    @AttributeDefinition(name = "TemperatureSensors", description = "Temperaturesensors to overlook the Temperature.")
    String[] thermometer() default {"TemperatureSensor0", "TemperatureSensor1"};

    @AttributeDefinition(name = "Reference Thermometer", description = "Reference Thermometer to be a start/endpoint for activation")
    String referenceThermometer() default "NotDefined";

    @AttributeDefinition(name = "TemperatureSensors at SecondaryHeater", description = "Like Primary/Secondary Forward/Rewind-->ORDER: pF,pR,sF,sR")
    String [] primaryAndSecondary() default {"NotDefined"};

    @AttributeDefinition(name = "ErrorInputHeater Type 2", description = "ErrorInputs via SignalSensorSpi for Heater Type 2 ( e.g. BiomassHeater).")
    String[] errorInputHeater1() default {"SignalSensorSpi3", "SignalSensorSpi4"};

    @AttributeDefinition(name = "ErrorInputHeater Type 1", description = "ErrorInputs via SignalSensorsSpi for Heater Type 1 ( e.g. GasBoiler)")
    String[] backUpPercentHeater2Error() default {"SignalSensorSpi0", "SignalSensorSpi1", "SignalSensorSpi2"};

    @AttributeDefinition(name = "Heat Mixer (== Valve)", description = "Valve to connect to")
    String valve() default "Valve0";

    @AttributeDefinition(name = "HeaterIds Controlled by Relay or 10V", description = "Id of Devices on Relay or 0-10V module.")
    String[] heaters() default {"Relay0", "LucidControlDeviceOutput1", "LucidControlDeviceOutput2"};

    @AttributeDefinition(name = "Timeout", description = "After which time Should the Controller definetly deactivate. Time in s")
            int sleepTime() default 1000;

    @AttributeDefinition(name = "Temperature Reference SetPoint Offset", description = "SetPoint Temperature When the Controller should Activate offset to controller")
    int activationTempOffset() default 5;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Heatnetwork PerformanceBooster [{id}]";
}