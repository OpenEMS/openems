package io.openems.edge.core.componentmanager;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "ComponentManager.ComponentManagerImpl", //
        description = "Configures the Metadata User Based provider")
@interface Config {

    @AttributeDefinition(name = "Path", description = "The path to the JSON file with the configuration.")
    String path();

    String webconsole_configurationFactory_nameHint() default "ComponentManager.ComponentManagerImpl";

}
