package io.openems.edge.timedata.influxdb;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Timedata InfluxDB", //
		description = "This component persists all data to an InfluxDB timeseries database.")
@interface Config {
	String id() default "influx0";

	boolean enabled() default true;

	@AttributeDefinition(name = "IP address", description = "IP address of InfluxDB server.")
	String ip() default "localhost";

	@AttributeDefinition(name = "TCP Port", description = "TCP Port of InfluxDB server.")
	int port() default 8086;

	@AttributeDefinition(name = "Username", description = "Username of InfluxDB server.")
	String username() default "root";

	@AttributeDefinition(name = "Password", description = "Password of InfluxDB server.")
	String password() default "root";

	@AttributeDefinition(name = "Database", description = "Database name of InfluxDB server.")
	String database() default "db";

	String webconsole_configurationFactory_nameHint() default "Timedata InfluxDB [{id}]";
}