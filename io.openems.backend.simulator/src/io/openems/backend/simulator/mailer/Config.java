package io.openems.backend.simulator.mailer;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Simulator.Mailer", //
		description = "Simple Mailer implementation, which writes 'Mails' into console.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "SimulatorMail";

	String webconsole_configurationFactory_nameHint() default "Simulator Mailer";

}