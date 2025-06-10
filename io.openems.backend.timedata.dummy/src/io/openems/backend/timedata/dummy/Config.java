package io.openems.backend.timedata.dummy;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Timedata.Dummy", //
		description = "Configures the Dummy timedata provider")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timedata0";

	String webconsole_configurationFactory_nameHint() default "Timedata Dummy";

}
