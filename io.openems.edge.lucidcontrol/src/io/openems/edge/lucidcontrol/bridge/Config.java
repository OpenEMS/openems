package io.openems.edge.lucidcontrol.bridge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Bridge LucidControl",
        description = "Bridge to communicate with the LucidControls Connected via USB."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "LucidControlBridge-ID", description = "ID of Lucid bridge.")
    String id() default "LucidBridge";

    @AttributeDefinition(name = "Path to LucidIoControl Software", description = "Complete Path to LucidIoControl Software")
    String lucidIoPath() default "/home/name/bin/64Bit/LucidIoCtrl";

    @AttributeDefinition(name = "Alias", description = "Human readable Name.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Lucid Bridge[{id}]";
}