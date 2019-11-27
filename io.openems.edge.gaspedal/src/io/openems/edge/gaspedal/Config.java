package io.openems.edge.gaspedal;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Consolinno BhkW Module",
        description = "After Activasion it's possible to activate the Bhkw Devices."
)

@interface Config {
    String service_pid();

    @AttributeDefinition(name = "Gaspedal Name", description = "Name of the Gaspedal Board.")
    String id() default "BhkWModule0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Board.")
    String alias() default "";

    @AttributeDefinition(name = "VersionNumber", description = "What Version of relaisBoard you are using.",
    options = @Option(label = "Version 1.0", value = "1"))
    String version() default "1";

    @AttributeDefinition(name = "Address", description = "The allocated address of the Module.")
    String address() default "0x60";

    @AttributeDefinition(name = "Bus Device", description = "The Bus you want to use on your device; Raspberry pi 3 only supports 1.")
    int bus() default 1;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Gaspedal [{id}]";

}


