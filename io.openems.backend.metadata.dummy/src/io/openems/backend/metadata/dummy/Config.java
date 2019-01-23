package io.openems.backend.metadata.dummy;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Metadata.Dummy", //
		description = "Configures the Metadata Dummy provider")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Metadata Dummy";

}
