package io.openems.edge.timedata.influxdb;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Timedata InfluxDB", //
		description = "This component persists all data to an InfluxDB timeseries database.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "influx0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP address", description = "IP address of InfluxDB server.")
	String ip() default "localhost";

	@AttributeDefinition(name = "TCP Port", description = "TCP Port of InfluxDB server.")
	int port() default 8086;

	@AttributeDefinition(name = "No of Cycles", description = "How many Cycles till data is written to InfluxDB.")
	int noOfCycles() default 1;

	@AttributeDefinition(name = "Username", description = "Username of InfluxDB server.")
	String username() default "root";

	@AttributeDefinition(name = "Password", description = "Password of InfluxDB server.")
	String password() default "root";

	@AttributeDefinition(name = "Database", description = "Database name of InfluxDB server.")
	String database() default "db";

	@AttributeDefinition(name = "Retention-Policy", description = "The InfluxDB retention policy")
	String retentionPolicy() default "autogen";

	@AttributeDefinition(name = "Read-Only mode", description = "Activates the read-only mode. Then no data is written to InfluxDB.")
	boolean isReadOnly() default false;

	String webconsole_configurationFactory_nameHint() default "Timedata InfluxDB [{id}]";
}