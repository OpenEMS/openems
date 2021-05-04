package io.openems.edge.lucidcontrol.device;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "LucidControl Input Device",
        description = "LucidControl Device, connected with the LucidControl Module."
)
@interface InputConfig {

    String service_pid();

    @AttributeDefinition(name = "LucidControlDevice-ID", description = "ID of LucidControlDevice.")
    String id() default "LucidControlInputDevice0";

    @AttributeDefinition(name = "Alias", description = "Human readable Name.")
    String alias() default "PressureMeterInput";

    @AttributeDefinition(name = "LucidControlModule-ID", description = "ID of LucidControlModule the device is connected to.")
    String moduleId() default "LucidControlInputModule0";

    @AttributeDefinition(name = "Position", description = "Position of Device (0-3)")
    int pinPos() default 0;

    @AttributeDefinition(name = "MaxBar", description = "How much pressure(in bar) is max. measured")
            double maxPressure() default 200;
    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "LucidControlDevice[{id}]";
}