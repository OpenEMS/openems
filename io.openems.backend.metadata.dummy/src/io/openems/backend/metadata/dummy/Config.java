package io.openems.backend.metadata.dummy;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Metadata.Dummy", //
		description = "Configures the Metadata Dummy provider")
@interface Config {

	@AttributeDefinition(name = "Edge-ID template", description = "Template for Edge-IDs, defaults to 'edge%d'")
	String edgeIdTemplate() default "edge%d";

	@AttributeDefinition(name = "Max Edge-ID", description = "Default predefines Edge-IDs from 'edge0' to 'edge10'")
	int edgeIdMax() default 10;

	String webconsole_configurationFactory_nameHint() default "Metadata Dummy";

}
