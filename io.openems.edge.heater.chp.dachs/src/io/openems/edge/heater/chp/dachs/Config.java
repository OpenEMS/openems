package io.openems.edge.heater.chp.dachs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Chp Dachs GLT-Interface",
        description = "Implements the Senertec Dachs Chp.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "Chp0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

    @AttributeDefinition(name = "IP address", description = "IP address of the GLT web interface")
    String address() default "localhost";

    @AttributeDefinition(name = "Username", description = "Username for the GLT web interface")
    String username() default "glt";

    @AttributeDefinition(name = "Password", description = "Password for the GLT web interface")
    String password() default "";

    @AttributeDefinition(name = "Polling interval [s]", description = "Unit: seconds. Time between calls to the GLT interface to update the values. Maximum 540.")
    int interval() default 10;
    
    @AttributeDefinition(name = "Write info to log", description = "Write basic data to log.")
    boolean basicInfo() default false;
    
    @AttributeDefinition(name = "Debug", description = "Write debug messages to log.")
    boolean debug() default false;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Chp Dachs GLT-Interface [{id}]";
}