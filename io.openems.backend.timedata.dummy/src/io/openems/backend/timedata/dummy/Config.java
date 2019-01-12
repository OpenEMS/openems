package io.openems.backend.timedata.dummy;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Timedata.Dummy", //
		description = "Configures the Dummy timedata provider")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Timedata Dummy";

}
