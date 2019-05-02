package io.openems.edge.core.componentmanager;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Secure Component Management", //
        description = "Configures the ComponentManager Secure provider")
@interface Config {

    @AttributeDefinition(name = "Path", description = "The path to the JSON file with the configuration.")
    String path();

    String webconsole_configurationFactory_nameHint() default "ComponentManager.ComponentManagerSecure";

}
