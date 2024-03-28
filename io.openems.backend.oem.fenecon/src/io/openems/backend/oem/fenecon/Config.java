package io.openems.backend.oem.fenecon;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "OEM FENECON", //
		description = "OEM FENECON." //
)
@interface Config {

	@AttributeDefinition(name = "Demo user ids", description = "Contains the demo userIds to anonymize edge metadata")
	String[] demoUserIds() default {};

	@AttributeDefinition(name = "AppCenter Master-Key", description = "AppCenter master-key used for admins to install apps", type = AttributeType.PASSWORD)
	String appCenterMasterKey();

	String webconsole_configurationFactory_nameHint() default "OEM FENECON";
}
