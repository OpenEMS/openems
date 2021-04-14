package io.openems.edge.heater.decentral;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Heater Decentral",
        description = "A Valve controlled by 2 relays used in the passing station."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Valve Name", description = "Unique Id of the Valve.")
    String id() default "DecentralHeater0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "ValveOrValveController", description = "Control a Valve directly or via Controller",
            options = {
                    @Option(label = "ValveController", value = "ValveController"),
                    @Option(label = "Valve", value = "Valve")}
    )
    String valveOrController() default "ValveController";

    @AttributeDefinition(name = "ValveOrControllerId", description = "The Valve that will be controlled, or the Controller you want to set")
    String valveOrControllerId() default "StaticValveController0";

    @AttributeDefinition(name = "ThresholdThermometerId", description = "ThresholdThermometer to Check")
    String thresholdThermometerId() default "ThresholdThermometer0";

    @AttributeDefinition(name = "SetPointTemperature", description = "Setpoint to OpenValve, only important if you control Valve directly and not via controller, Unit: Decidegree: 1°C = 10dC")
    int setPointTemperature() default 700;

    int maximumThermalOutputInKw() default 150;

    boolean shouldCloseOnActivation() default true;

    @AttributeDefinition(name = "Force Heating", description = "On HeaterEnabled Signal -> Force Heating no matter if the Response/Callback says ok or not")
    boolean forceHeating() default false;

    @AttributeDefinition(name = "Cycles to wait when Need Heat Enable Signal (Central Controller) isn't present", description = "How many Cycles do you wait for the Central Communication Controller if Communication is lost")
    int waitCyclesNeedHeatResponse() default 8;

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Heater Decentral [{id}]";
}