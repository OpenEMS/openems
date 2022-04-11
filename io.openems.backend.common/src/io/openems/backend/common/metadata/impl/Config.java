package io.openems.backend.common.metadata.impl;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Metadata.Backend", //
		description = "Configures the Backend Metadata provider")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Metadata.Backend";

}
