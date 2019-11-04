package io.openems.edge.relais;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Consolinno Relais",
        description = "Relais with a Channel to Open and Close"
)

@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Relais Name", description = "")
    String id() default "relais0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "Relais Type", description = "Is the Relais an Opener or closer Opener",
            options = {
                    @Option(label = "Opener", value = "Opener"),
                    @Option(label = "Closer", value = "Closer"),
                    @Option(label = "Reverse", value = "Reverse")
            })
    String relaisType() default "Opener";

    @AttributeDefinition(name = "I2C Bridge - ID", description = "ID of I2C Bridge - ID.")
    String spiI2c_id() default "I2C0";

    @AttributeDefinition(name = "RelaisBoard ID", description = "Id of relaisboard allocated to this relais.")
    String relaisBoard_id() default "relaisBoard0";

    @AttributeDefinition(name = "Position", description = "The position of the Relais. Starting with 0")
    int position();

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Actuator Relais [{id}]";
}