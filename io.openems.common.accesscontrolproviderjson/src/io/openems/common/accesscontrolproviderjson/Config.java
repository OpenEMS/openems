package io.openems.common.accesscontrolproviderjson;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Access Control Provider Json", //
        description = "Can be configured for activating the Access Control")
@interface Config {

    @AttributeDefinition(name = "Path", description = "The path to the JSON file with the configuration.")
    String path();

    @AttributeDefinition(name = "Priority", description = "The priority of the provider. The providers will fill the access control sorted ascending after their priority")
    int priority();

    String webconsole_configurationFactory_nameHint() default "AccessControlProvider.AccessControlProviderJson";

}
