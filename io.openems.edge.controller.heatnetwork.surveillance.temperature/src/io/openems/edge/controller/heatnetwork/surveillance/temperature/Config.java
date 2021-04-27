package io.openems.edge.controller.heatnetwork.surveillance.temperature;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Controller Temperature Surveillance", description = "Temperature Surveillance Controller: On Certain Temperatures Control Valve/Heater")
@interface Config {
    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for this Controller.")
    String id() default "TemperatureSurveillance0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Controller.")
    String alias() default "";

    @AttributeDefinition(name = "ReferenceThermometer", description = "Own Thermometer to compare itself to other Thermometer")
    String referenceThermometerId() default "TemperatureSensor0";

    @AttributeDefinition(name = "Thermometer Activate Id", description = "Activation Thermometer, if reference beneath this Thermometer -> activate")
    String thermometerActivateId() default "ThermometerVirtual0";

    @AttributeDefinition(name = "TemperatureOffset Activate", description = "If Reference < ActivateThermometer + Offset --> Control Components; Offset in dC")
    int offsetActivate() default 100;



    @AttributeDefinition(name = "Thermometer Deactivate", description = "Deactivation Thermometer")
    String thermometerDeactivateId() default "ThresholdThermometer1";

    @AttributeDefinition(name = "TemperatureOffset Deactivate", description = "If Reference > Deactivate thermometer + Offset --> Deactivate: Unit dC")
    int offsetDeactivate() default -100;

    @AttributeDefinition(name = "Use ValveController")
    boolean useValveController() default true;

    @AttributeDefinition(name = "ValveControllerId", description = "Unique Id of the ValveController you want to use")
    String valveControllerId() default "ValveController0";

    @AttributeDefinition(name = "Use Heater")
    boolean useHeater() default true;

    @AttributeDefinition(name = "Heater Id")
    String heaterId() default "Heater0";

    @AttributeDefinition(name = "WaitTime in seconds", description = "How long to Wait till Valve Opens (after Heat activation) t in seconds")
    int timeToWait() default 120;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Temperature Surveillance [{id}]";
}
