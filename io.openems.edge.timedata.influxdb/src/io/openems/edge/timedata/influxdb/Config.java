package io.openems.edge.timedata.influxdb;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.channel.PersistencePriority;
import io.openems.shared.influxdb.QueryLanguageConfig;

@ObjectClassDefinition(//
		name = "Timedata InfluxDB", //
		description = "This component persists all data to an InfluxDB timeseries database.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "influx0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Query language", description = "Query language Flux or InfluxQL")
	QueryLanguageConfig queryLanguage() default QueryLanguageConfig.INFLUX_QL;

	@AttributeDefinition(name = "URL", description = "The InfluxDB URL, e.g.: http://localhost:8086")
	String url() default "http://localhost:8086";

	@AttributeDefinition(name = "Org", description = "The Organisation; for InfluxDB v1: '-'")
	String org() default "-";

	@AttributeDefinition(name = "ApiKey", description = "The ApiKey; for InfluxDB v1: 'username:password', e.g. 'admin:admin'")
	String apiKey();

	@AttributeDefinition(name = "Bucket", description = "The bucket name; for InfluxDB v1: 'database/retentionPolicy', e.g. 'db/data'")
	String bucket();

	@AttributeDefinition(name = "Measurement", description = "The InfluxDB measurement")
	String measurement() default "data";

	@AttributeDefinition(name = "No of Cycles", description = "How many Cycles till data is written to InfluxDB.")
	int noOfCycles() default 1;

	@AttributeDefinition(name = "Number of max scheduled tasks", description = "Max-Size of Queued tasks.")
	int maxQueueSize() default 5000;

	@AttributeDefinition(name = "Read-Only mode", description = "Activates the read-only mode. Then no data is written to InfluxDB.")
	boolean isReadOnly() default false;
	
	@AttributeDefinition(name = "Persistence Priority", description = "Store only Channels with a Persistence Priority above this. Be aware that too many writes can wear-out your flash storage.")
	PersistencePriority persistencePriority() default PersistencePriority.MEDIUM;	

	String webconsole_configurationFactory_nameHint() default "Timedata InfluxDB [{id}]";
}