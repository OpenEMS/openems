package io.openems.backend.metadata.odoo;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.backend.metadata.odoo.odoo.Protocol;
import io.openems.common.websocket.AbstractWebsocketServer.DebugMode;

@ObjectClassDefinition(//
		name = "Metadata.Odoo", //
		description = "Configures the Odoo Metadata provider")
public @interface Config {

	@AttributeDefinition(name = "Odoo Protocol", description = "The odoo protocol")
	Protocol odooProtocol() default Protocol.HTTP;

	@AttributeDefinition(name = "Odoo Host", description = "The odoo host")
	String odooHost() default "localhost";

	@AttributeDefinition(name = "Odoo Port", description = "The odoo port")
	int odooPort() default 8069;

	@AttributeDefinition(name = "Odoo UID", description = "The odoo login UID")
	int odooUid() default 1;

	@AttributeDefinition(name = "Odoo Password", description = "The odoo login password")
	String odooPassword();

	@AttributeDefinition(name = "Postgres Host", description = "The Postgres host")
	String pgHost() default "localhost";

	@AttributeDefinition(name = "Postgres Port", description = "The Postgres port")
	int pgPort() default 5432;

	@AttributeDefinition(name = "Postgres Username", description = "The Postgres username")
	String pgUser() default "odoo";

	@AttributeDefinition(name = "Postgres Password", description = "The Postgres password")
	String pgPassword();

	@AttributeDefinition(name = "Database", description = "The database name")
	String database();

	@AttributeDefinition(name = "Number of Threads", description = "Pool-Size: the number of threads dedicated to handle the tasks")
	int poolSize() default 30;

	@AttributeDefinition(name = "Number of Threads", description = "Pool-Size: the maximum number of concurrent connections")
	int pgConnectionPoolSize() default 40;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	DebugMode debugMode() default DebugMode.OFF;

	String webconsole_configurationFactory_nameHint() default "Metadata.Odoo";

}
