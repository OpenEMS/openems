package io.openems.backend.metadata.energydepot;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Metadata.EnergyDepot", //
		description = "Configures the Metadata Energy Depot provider")
@interface Config {
	@AttributeDefinition(name = "Password", description = "DB Password")
	String password();

	String webconsole_configurationFactory_nameHint() default "Metadata.EnergyDepot";
}
