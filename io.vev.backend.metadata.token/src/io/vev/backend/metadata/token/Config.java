package io.vev.backend.metadata.token;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Metadata.Token", //
		description = "Configures the Metadata Vev-Token provider")
@interface Config {

	@AttributeDefinition(name = "Edge-ID template", description = "Template for Edge-IDs, defaults to 'edge%d'")
	String edgeIdTemplate() default "edge%d";

	@AttributeDefinition(name = "Max Edge-ID", description = "Default predefines Edge-IDs from 'edge0' to 'edge10'")
	int edgeIdMax() default 10;

	@AttributeDefinition(name = "MongoDB connection URI", description = "Connection string used to access MongoDB")
	String mongoUri() default "mongodb://localhost:27017";

	@AttributeDefinition(name = "MongoDB database", description = "Database name containing metadata collections")
	String mongoDatabase() default "evse-db";


	String webconsole_configurationFactory_nameHint() default "Metadata Token";

}
