package io.openems.backend.timedata.timescaledb;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Timedata.TimescaleDB", //
		description = "Configures the TimescaleDB Timedata provider")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timedata0";

	@AttributeDefinition(name = "Host", description = "The TimescaleDB/PostgresDB host")
	String host() default "localhost";

	@AttributeDefinition(name = "Port", description = "The TimescaleDB/PostgresDB port")
	int port() default 5432;

	@AttributeDefinition(name = "Username", description = "The TimescaleDB/PostgresDB username")
	String user();

	@AttributeDefinition(name = "Password", description = "The TimescaleDB/PostgresDB password", type = AttributeType.PASSWORD)
	String password();

	@AttributeDefinition(name = "Database", description = "The TimescaleDB/PostgresDB database name")
	String database();

	@AttributeDefinition(name = "Read-Only mode", description = "Activates the read-only mode. Then no data is written to TimescaleDB.")
	boolean isReadOnly() default false;

	@AttributeDefinition(name = "Number of Threads", description = "Pool-Size: the number of threads dedicated to handle the tasks")
	int poolSize() default 10;

	@AttributeDefinition(name = "Enable Write for Channel-Addresses", description = "")
	String[] enableWriteChannelAddresses();

	String webconsole_configurationFactory_nameHint() default "Timedata.TimescaleDB";

}
