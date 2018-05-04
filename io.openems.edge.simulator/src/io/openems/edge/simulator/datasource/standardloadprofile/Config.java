package io.openems.edge.simulator.datasource.standardloadprofile;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Simulator DataSource: Standard Load Profile", //
		description = "This service provides Standard Load Profile data.")
@interface Config {
	String service_pid();

	String id() default "datasource0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Source", description = "The standard load profile to be used.")
	Source source();

	String webconsole_configurationFactory_nameHint() default "Simulator DataSource: Standard Load Profile [{id}]";
}