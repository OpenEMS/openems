package io.openems.backend.metadata.file;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Metadata.File", //
		description = "Configures the Metadata File provider")
@interface Config {

	@AttributeDefinition(name = "Path", description = "The path to the JSON file with the configuration.")
	String path();

	String webconsole_configurationFactory_nameHint() default "Metadata.File";

}
