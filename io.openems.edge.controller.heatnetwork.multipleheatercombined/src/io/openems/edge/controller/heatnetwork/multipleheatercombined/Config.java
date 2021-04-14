package io.openems.edge.controller.heatnetwork.multipleheatercombined;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Consolinno Multiple Heater",
        description = "This Controller regulates the Pump and Valves for Heating."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the Controller.")
    String id() default "MultipleHeaterCombined0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Controller.")
    String alias() default "";

    boolean usePrimaryHeater() default true;

    @AttributeDefinition(name = "Heating Device 1 Name", description = "Unique Id of the first Heating Device.")
    String primaryHeaterId() default "Chp0";

    @AttributeDefinition(name = "Heating Device 1 MIN Temperature in dC", description = "Threshold of the Heating Device 1 should be turned ON(in dC --> 1°C == 10°dC).")
    int primaryHeaterMinTemperature() default 600;

    @AttributeDefinition(name = "Heating Device 1 MAX Temperature in dC", description = "Threshold of the Heating Device 1 should be turned OFF(in dC --> 1°C == 10°dC).")
    int primaryHeaterMaxTemperature() default 800;

    @AttributeDefinition(name = "HeatingDevice 1 TemperatureSensor MIN", description = "The Temperature-Sensor for the Heating Device 1 Temperature MIN.")
    String primaryTemperatureSensorMin() default "TemperatureSensor0";

    @AttributeDefinition(name = "HeatingDevice 1 TemperatureSensor MAX", description = "The Temperature-Sensor for the Heating Device 1 Temperature MAX.")
    String primaryTemperatureSensorMax() default "TemperatureSensor1";

    boolean useSecondaryHeater() default true;

    @AttributeDefinition(name = "Heating Device 2 Name", description = "Unique Id of the second Heating Device.")
    String secondaryHeaterId() default "WoodChipHeater0";

    @AttributeDefinition(name = "Heating Device 2 MIN Temperature in dC", description = "Threshold of the Heating Device 2 should be turned ON(in dC --> 1°C == 10°dC).")
    int secondaryTemperatureMin() default 600;

    @AttributeDefinition(name = "Heating Device 2 MAX Temperature in dC", description = "Threshold of the Heating Device 2 should be turned OFF(in dC --> 1°C == 10°dC).")
    int secondaryTemperatureMax() default 800;

    @AttributeDefinition(name = "HeatingDevice 2 TemperatureSensor MIN", description = "The Temperature-Sensor for the Heating Device 2 Temperature MIN.")
    String secondaryTemperatureSensorMin() default "TemperatureSensor2";

    @AttributeDefinition(name = "HeatingDevice 2 TemperatureSensor MAX", description = "The Temperature-Sensor for the Heating Device 2 Temperature MAX.")
    String secondaryTemperatureSensorMax() default "TemperatureSensor3";

    boolean useTertiaryHeater() default true;

    @AttributeDefinition(name = "Heating Device 3 Name", description = "Unique Id of the third Heating Device.")
    String tertiaryHeaterId() default "GasBoiler0";

    @AttributeDefinition(name = "Heating Device 3 MIN Temperature in dC", description = "Threshold of the Heating Device 3 should be turned ON(in dC --> 1°C == 10°dC).")
    int tertiaryTemperatureMin() default 600;

    @AttributeDefinition(name = "Heating Device 3 MAX Temperature in dC", description = "Threshold of the Heating Device 3 should be turned OFF(in dC --> 1°C == 10°dC).")
    int tertiaryTemperatureMax() default 800;

    @AttributeDefinition(name = "HeatingDevice 3 TemperatureSensor MIN", description = "The Temperature-Sensor for the Heating Device 3 Temperature MIN.")
    String tertiaryTemperatureSensorMin() default "TemperatureSensor4";

    @AttributeDefinition(name = "HeatingDevice 3 TemperatureSensor MAX", description = "The Temperature-Sensor for the Heating Device 3 Temperature MAX.")
    String tertiaryTemperatureSensorMax() default "TemperatureSensor5";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Multiple Heater Combined [{id}]";

}