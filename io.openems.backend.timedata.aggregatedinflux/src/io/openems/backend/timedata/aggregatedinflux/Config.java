package io.openems.backend.timedata.aggregatedinflux;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.shared.influxdb.QueryLanguageConfig;

@ObjectClassDefinition(//
		name = "Timedata.AggregatedInfluxDB", //
		description = "Configures the InfluxDB timedata provider" //
)
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timedata0";

	@AttributeDefinition(name = "Query language", description = "Query language Flux or InfluxQL")
	QueryLanguageConfig queryLanguage() default QueryLanguageConfig.INFLUX_QL;

	@AttributeDefinition(name = "URL", description = "The InfluxDB URL, e.g.: http://ip:port")
	String url();

	@AttributeDefinition(name = "Org", description = "The Organisation; '-' for InfluxDB v1")
	String org() default "-";

	@AttributeDefinition(name = "ApiKey", description = "The ApiKey; 'username:password' for InfluxDB v1")
	String apiKey();

	@AttributeDefinition(name = "Bucket", description = "The bucket name; 'database' for InfluxDB v1")
	String bucket();

	@AttributeDefinition(name = "Retention policy name for average values", description = "The retention policy name for InfluxDB v1 for daily values")
	String retentionPolicyAvg() default "rp_avg";

	@AttributeDefinition(name = "Retention policy name for max values", description = "The retention policy name for InfluxDB v1 for monthly values")
	String retentionPolicyMax() default "rp_max";

	@AttributeDefinition(name = "Measurement avg", description = "The InfluxDB measurement for average values")
	String measurementAvg() default "avg";

	@AttributeDefinition(name = "Measurements for max values", description = "Measurements for max values for each timezone. Format: \"(timezone)=(measurement)\"")
	String[] measurementsMax() default { "Europe/Berlin=max" };

	@AttributeDefinition(name = "Read-Only mode", description = "Activates the read-only mode. Then no data is written to InfluxDB.")
	boolean isReadOnly() default false;

	@AttributeDefinition(name = "Number of Threads", description = "Pool-Size: the number of threads dedicated to handle the tasks")
	int poolSize() default 10;

	@AttributeDefinition(name = "Number of max scheduled tasks", description = "Max-Size of Queued tasks.")
	int maxQueueSize() default 5000;

	String webconsole_configurationFactory_nameHint() default "Timedata Aggregated InfluxDB";

}
