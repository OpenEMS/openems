package io.openems.backend.core.timedatamanager;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core Timedata-Manager", //
		description = "The global manager for Timedata Services")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Core Timedata-Manager";

	@AttributeDefinition(name = "Timedata-IDs", description = "IDs of Timedata Services. Execution is going to be sorted in the order of the IDs.")
	String[] timedata_ids() default {};

}