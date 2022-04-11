package io.openems.backend.metadata.edge.file;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "EdgeMetadata.File", //
		description = "Configures the EdgeMetadata.File provider")
@interface Config {
	
	@AttributeDefinition(name = "Path", description = "The path to the JSON file.")
	String path();

	String webconsole_configurationFactory_nameHint() default "EdgeMetadata.File";

}
