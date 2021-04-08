package io.openems.edge.timedata.mssql;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Timedata MS-SQL", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "mssql0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Server name",  description = "Server name or a.k.a Host name of the MS-SQL DB")
	String server() default "LocalHost";
	
	@AttributeDefinition(name = "Port number", description = "Port number at which MS-SQL DB is available")
	int port() default 1433;
	
	@AttributeDefinition(name = "Database name", description = "Database name for the connection")
	String dbname() default "";
	
	@AttributeDefinition(name = "No of Cycles", description = "How many Cycles till data is written to MS-SQL DB.")
	int noOfCycles() default 1;
	
	@AttributeDefinition(name = " Is password protected?", description = "Is the MS-SQL DB password protected")
	boolean isPasswordProtected() default true;
	
	@AttributeDefinition(name = "Username", description = "Username for logging into the DB (Mandatory when DB is password protected!)")
	String username() default "admin";
	
	@AttributeDefinition(name = "Password", description = "Password for logging into the DB (Mandatory when DB is password protected!)")
	String password() default "admin";
	
	@AttributeDefinition(name = "Read-Only mode", description = "Activates the read-only mode. Then no data is written to MS-SQL DB.")
	boolean isReadOnly() default false;

	String webconsole_configurationFactory_nameHint() default "Timedata MS-SQL [{id}]";

}