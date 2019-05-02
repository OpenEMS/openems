package io.openems.edge.core.componentmanager.basic;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Basic Component Management", //
        description = "Configures the ComponentManager Basic provider")
@interface Config {

    String webconsole_configurationFactory_nameHint() default "ComponentManager.ComponentManagerSecure";
}
