package io.openems.backend.metadata.odoo;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Metadata.Odoo", //
		description = "Configures the Odoo Metadata provider")
@interface Config {
	@AttributeDefinition(name = "Database", description = "The database name")
	String database();

	@AttributeDefinition(name = "UID", description = "The odoo login UID")
	int uid();

	@AttributeDefinition(name = "Password", description = "The odoo login password")
	String password();

	@AttributeDefinition(name = "URL", description = "The odoo URL")
	String url();
	
	String webconsole_configurationFactory_nameHint() default "Metadata.Odoo";
}
