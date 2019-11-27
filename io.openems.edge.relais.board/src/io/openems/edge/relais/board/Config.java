package io.openems.edge.relais.board;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Consolinno Board Relais",
        description = "Depending on VersionId you can activate up to X Relais on this CircuitBoard"
)

@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Relais Board Name", description = "Name of the Relais Board")
    String id() default "relaisBoard0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Board")
    String alias() default "";

    @AttributeDefinition(name = "VersionNumber", description = "What Version of relaisBoard you are using.",
    options = @Option(label = "Version 1.0", value = "1"))
    String version() default "1";

    @AttributeDefinition(name = "Address", description = "Address you want to use between 0x20/22/24/26")
            String address() default "0x20";

     @AttributeDefinition(name = "Bus Device", description = "What Channel you want to use.")
             int bus() default 1;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Relais Board [{id}]";
}