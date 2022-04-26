package io.openems.backend.alerting;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Alerting", //
		description = "Configures the Notification Service")
public @interface Config {

	String webconsole_configurationFactory_nameHint() default "Alerting";

}
