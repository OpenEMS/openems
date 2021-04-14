package io.openems.edge.controller.pump.grundfos;

import io.openems.edge.controller.pump.grundfos.api.ControlModeSetting;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Controller Pump Grundfos", //
        description = "Controller to operate a Gundfos pump over GENIbus.  IMPORTANT: This module requires "
                + "\"Bridge GeniBus\" and \"Pump Grundfos\" to be active. It won't start if that is not the case!")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "ControllerPumpGrundfos0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "PumpId", description = "Unique Id of the Pump.")
    String pumpId() default "Pump0";

    @AttributeDefinition(name = "Control mode", description = "Control mode - 'constant pressure' or 'constant frequency'")
    ControlModeSetting controlMode() default ControlModeSetting.CONST_PRESSURE;

    @AttributeDefinition(name = "Pressure setpoint (pumping head, Förderhöhe)",
            description = "Unit is bar. Conversion for pumping head (Förderhöhe): 1 m = 0.1 Bar. Only used when in constant pressure mode.")
    double pressureSetpoint() default 0.2;

    @AttributeDefinition(name = "Frequency setpoint",
            description = "Unit is percent of maximum. E.g. 100 for full speed. Can't go below minimum frequency (52%). "
                    + "Only used when in constant frequency mode.")
    double frequencySetpoint() default 50;

    @AttributeDefinition(name = "Stop the pump", description = "Stops the pump")
    boolean stopPump() default false;

    @AttributeDefinition(name = "Write pump status to log", description = "Write pump status parameters in the log.")
    boolean printPumpStatus() default false;

    @AttributeDefinition(name = "Read only", description = "Only reads values from the pump, do not send commands.")
    boolean onlyRead() default false;

    String webconsole_configurationFactory_nameHint() default "Controller Pump Grundfos [{id}]";
}