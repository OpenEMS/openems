package io.openems.backend.metadata.wordpress;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Metadata.EnergyDepot", //
		description = "Configures the Metadata Wordpress provider")
@interface Config {
	@AttributeDefinition(name = "User", description = "DB User")
	String user() default "root2";
	
	@AttributeDefinition(name = "Password", description = "DB Password")
	String password();
	
	@AttributeDefinition(name = "DB URL", description = "DB Server URL incl port")
	String dburl() default "jdbc:mariadb://localhost:3306";
	
	@AttributeDefinition(name = "DB URL", description = "DB Server URL incl port")
	String dbname() default "primus";
	
	@AttributeDefinition(name = "Wordpress URL", description = "Wordpress URL for API use")
	String wpurl() default "https://www.energydepot.de";

	String webconsole_configurationFactory_nameHint() default "Metadata.Wordpress";
}
