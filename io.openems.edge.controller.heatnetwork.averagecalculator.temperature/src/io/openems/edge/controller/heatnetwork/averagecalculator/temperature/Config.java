package io.openems.edge.controller.heatnetwork.averagecalculator.temperature;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Temperature AverageCalculation",
        description = "This Controller Calculates the average temperature and writes the result in a virtual Thermometer."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the Passing Controller.")
    String id() default "ControllerAverageCalculation";

    @AttributeDefinition(name = "alias", description = "Human readable name of Controller.")
    String alias() default "";

    @AttributeDefinition(name = "ThermometerChannel", description = "Thermometers to calculate the AverageTemperature from")
            String[] readThermometerChannel() default {"TemperatureSensor0/Temperature"};
    @AttributeDefinition(name = "Channel of Virtual Thermometer", description = "VirtualThermometer to write Result of AverageCalculation into")
            String writeVirtualThermometerChannel() default "VirtualThermometer0/VirtualTemperature";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Temperature AverageCalculation";
}