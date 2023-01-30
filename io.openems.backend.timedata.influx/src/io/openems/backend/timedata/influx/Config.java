package io.openems.backend.timedata.influx;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Timedata.InfluxDB", //
		description = "Configures the InfluxDB timedata provider")
@interface Config {

	@AttributeDefinition(name = "URL", description = "The InfluxDB URL, e.g.: http://ip:port")
	String url();

	@AttributeDefinition(name = "Org", description = "The Organisation; '-' for InfluxDB v1")
	String org() default "-";

	@AttributeDefinition(name = "ApiKey", description = "The ApiKey; 'username:password' for InfluxDB v1")
	String apiKey();

	@AttributeDefinition(name = "Bucket", description = "The bucket name; 'database/retentionPolicy' for InfluxDB v1")
	String bucket();

	@AttributeDefinition(name = "Measurement", description = "The InfluxDB measurement")
	String measurement() default "data";

	@AttributeDefinition(name = "Use FLUX Queries?", description = "If enabled FLUX query language is used, else InfluxQL will be used")
	boolean useFluxQueries() default true;

	@AttributeDefinition(name = "Read-Only mode", description = "Activates the read-only mode. Then no data is written to InfluxDB.")
	boolean isReadOnly() default false;

	String webconsole_configurationFactory_nameHint() default "Timedata InfluxDB";

}
