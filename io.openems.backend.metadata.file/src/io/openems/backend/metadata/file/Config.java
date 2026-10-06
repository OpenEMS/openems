package io.openems.backend.metadata.file;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.session.Language;

@ObjectClassDefinition(//
		name = "Metadata.File", //
		description = "Configures the Metadata File provider")
@interface Config {

	@AttributeDefinition(name = "Path", description = "The path to the JSON file.")
	String path();

	@AttributeDefinition(name = "Default Language", description = "The default language for the shared user.")
	Language defaultLanguage() default Language.DE;

	String webconsole_configurationFactory_nameHint() default "Metadata.File";

}
