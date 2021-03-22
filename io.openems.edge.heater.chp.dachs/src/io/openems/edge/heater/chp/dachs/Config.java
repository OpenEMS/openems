package io.openems.edge.heater.chp.dachs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Consolinno Dachs GLT-Interface",
        description = "Module to communicate with a Dachs CHP using it's GLT web interface."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Interface Name", description = "Unique Name of the Interface.")
    String id() default "DachsGltInterface0";

    @AttributeDefinition(name = "alias", description = "Human Readable Name of Component.")
    String alias() default "Dachs GLT-Schnittstelle";

    @AttributeDefinition(name = "IP address", description = "IP address of the GLT web interface")
    String address() default "localhost";

    @AttributeDefinition(name = "Username", description = "Username for the GLT web interface")
    String username() default "glt";

    @AttributeDefinition(name = "Password", description = "Password for the GLT web interface")
    String password() default "";

    @AttributeDefinition(name = "Polling interval", description = "Unit: seconds. Time between calls of the GLT interface to updates the values. Maximum 540.")
    int interval() default 10;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Consolinno Dachs GLT-Interface [{id}]";
}