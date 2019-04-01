package io.openems.backend.metadata.user_based;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Metadata.UserBased", //
		description = "Configures the Metadata User Based provider")
@interface Config {

	@AttributeDefinition(name = "Path", description = "The path to the JSON file with the configuration.")
	String path();

	String webconsole_configurationFactory_nameHint() default "Metadata.UserBased";

}
