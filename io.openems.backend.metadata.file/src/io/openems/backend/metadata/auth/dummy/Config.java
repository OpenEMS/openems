package io.openems.backend.metadata.auth.dummy;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "AuthenticationMetadata.Dummy", //
		description = "Configures the AuthenticationMetadata.Dummy provider")
@interface Config {
	
	String webconsole_configurationFactory_nameHint() default "AuthenticationMetadata.Dummy";

}
