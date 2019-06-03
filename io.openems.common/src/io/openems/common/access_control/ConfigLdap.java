package io.openems.common.access_control;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Access Control Data Source Ldap", //
        description = "Can be configured for activating the Access Control")
@interface ConfigLdap {

    @AttributeDefinition(name = "Host", description = "The ldap server address")
    String host();

    @AttributeDefinition(name="Port", description = "The ldap server port")
    int port();

    String webconsole_configurationFactory_nameHint() default "AccessControlProvider.AccessControlProviderLdap";

}
