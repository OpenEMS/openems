package io.openems.edge.relaisBoard;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Consolinno RelaisBoard",
        description = "Depending on VersionId you can activate up to X Relais on this CircuitBoard"
)

@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Relais Board Name", description = "Name of the Relais Board")
    String id() default "relaisBoard0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Board")
    String alias() default "";

    @AttributeDefinition(name = "VersionNumber", description = "What Version of relaisBoard you are using.")
    String version() default "1";

    @AttributeDefinition(name = "I2CBridge Id", description = "Id of previously activated I2C Bridge.")
            String bridge() default "I2C0";

    @AttributeDefinition(name = "Address", description = "Address you want to use between 0x20-0x60")
            short address() default 0x20;

     @AttributeDefinition(name = "Bus Device", description = "What Channel you want to use.")
             int bus() default 0;

    String webconsole_configurationFactory_nameHint() default "Relais Board [{id}]";
}