package io.openems.edge.lucidcontrol.device;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "LucidControl Output Device",
        description = "LucidControl Device, connected with the LucidControl Module."
)
@interface OutputConfig {
    String service_pid();

    @AttributeDefinition(name = "LucidControlDevice-ID", description = "ID of LucidControlDevice.")
    String id() default "LucidControlDeviceOutput0";

    @AttributeDefinition(name = "Alias", description = "Human readable Name.")
    String alias() default "PressureMeterOutput";

    @AttributeDefinition(name = "LucidControlModule-ID", description = "ID of LucidControlModule where the Device is connected with.")
    String moduleId() default "LucidControlOutputModule0";

    @AttributeDefinition(name = "Position", description = "Position of Device (0-3)")
    int pinPos() default 0;

    @AttributeDefinition(name = "VoltageThreshold", description = "VoltageThreshold List: Given Value indicates extra Offset. Inputs as %")
    double[] voltageThreshold() default {50};

    @AttributeDefinition(name = "VoltageThreshold Value", description = "VoltageThreshold Offset Value added/subtracted from % Value")
    double[] voltageThresholdValue() default {0.5};

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "LucidControlDevice[{id}]";
}
