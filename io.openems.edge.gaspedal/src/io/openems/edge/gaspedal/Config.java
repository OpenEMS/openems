package io.openems.edge.gaspedal;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Consolinno Gaspedal",
        description = "Depending on VersionId you can activate up to X Devices per Gaspedal"
)

@interface Config {
    String service_pid();

    @AttributeDefinition(name = "Gaspedal Name", description = "Name of the Gaspedal Board")
    String id() default "Gaspedal0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Board")
    String alias() default "";

    @AttributeDefinition(name = "VersionNumber", description = "What Version of relaisBoard you are using.")
    String version() default "1";

    @AttributeDefinition(name = "I2CBridge Id", description = "Id of previously activated I2C Bridge.")
    String bridge() default "I2C0";

    @AttributeDefinition(name = "Address", description = "Adress you want to use default 0x60")
    String address() default "0x60";

    @AttributeDefinition(name = "Bus Device", description = "What Channel you want to use.")
    int bus() default 1;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Gaspedal [{id}]";

}


