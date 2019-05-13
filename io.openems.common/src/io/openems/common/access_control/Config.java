package io.openems.common.access_control;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Access Control Data Source Json", //
        description = "Can be configured for activating the Access Control")
@interface Config {

    @AttributeDefinition(name = "Path", description = "The path to the JSON file with the configuration.")
    String path();

    String webconsole_configurationFactory_nameHint() default "AccessControlDataSource.AccessControlDataSourceJson";

}
