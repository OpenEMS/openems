package io.openems.backend.alerting;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Alerting", //
		description = "Configures the Notification Service")
public @interface Config {

	String webconsole_configurationFactory_nameHint() default "Alerting";

	@AttributeDefinition(name = "Initial Delay", description = "Delay in minutes, after Backend start, before Offline-Edge detection starts.")
	int initialDelay() default 15;

}
