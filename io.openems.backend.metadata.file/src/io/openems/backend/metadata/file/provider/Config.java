package io.openems.backend.metadata.file.provider;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Metadata.File", //
		description = "Configures the Metadata File provider")
@interface Config {
	@AttributeDefinition(name = "Path", description = "The port of the CSV file.")
	String path();

	String webconsole_configurationFactory_nameHint() default "Metadata.File";
}
