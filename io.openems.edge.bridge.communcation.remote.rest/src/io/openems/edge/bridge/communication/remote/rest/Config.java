package io.openems.edge.bridge.communication.remote.rest;

import org.osgi.service.component.annotations.ComponentPropertyType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Bridge Rest",
        description = " Rest Bridge, needed for the communication with Master/Slave Devices e.g. Different Openems.")

@interface Config {
    String service_pid();

    @AttributeDefinition(name = "Rest Bridge - Id", description = "Id of Rest Bridge.")
    String id() default "RestBridge0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "Ip Address", description = "Ip Address of Device you want to communicate with.")
    String ipAddress() default "192.168.101.1";

    @AttributeDefinition(name = "Port", description = "Open Port of Device")
    String port() default "8084";

    @AttributeDefinition(name = "AuthorisationHeader - User", description = "UserName to access Device")
    String username() default "Admin";

    @AttributeDefinition(name = "Password", description = "Password for authorization")
    String password() default "";

    @AttributeDefinition(name = "Keep Alive", description = "Time Interval to Check for active Connection: Time in Seconds")
    int keepAlive() default 360;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Rest Bridge [{id}]";
}
