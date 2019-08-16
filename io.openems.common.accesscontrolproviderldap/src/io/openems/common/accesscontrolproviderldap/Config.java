package io.openems.common.accesscontrolproviderldap;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Access Control Data Source Ldap", //
        description = "Can be configured for activating the Access Control")
@interface Config {

    @AttributeDefinition(name = "Host", description = "The ldap server address")
    String host();

    @AttributeDefinition(name = "Port", description = "The ldap server port")
    int port();

    @AttributeDefinition(name = "Priority", description = "The priority of the provider. The providers will fill the access control sorted ascending after their priority")
    int priority();

    String webconsole_configurationFactory_nameHint() default "AccessControlProvider.AccessControlProviderLdap";

}
