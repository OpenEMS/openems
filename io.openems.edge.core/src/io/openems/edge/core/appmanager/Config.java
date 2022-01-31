package io.openems.edge.core.appmanager;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core App-Manager", //
		description = "The global manager for OpenEMS Apps.")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Core App-Manager";

	@AttributeDefinition(name = "OpenEMS Apps", description = "OpenEMS App properties as a JSON Array") //
	String apps() default "[]";

}