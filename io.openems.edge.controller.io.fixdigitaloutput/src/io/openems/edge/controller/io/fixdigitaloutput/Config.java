package io.openems.edge.controller.io.fixdigitaloutput;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller IO Fix Digital Output", //
		description = "This controller sets a digital output channel according to the given value")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlIoFixDigitalOutput0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Output Channel", description = "Channel address of the Digital Output that should be switched")
	String outputChannelAddress();

	@AttributeDefinition(name = "Is on", description = "If this option is activated the Digital Output is switched ON")
	boolean isOn() default false;

	String webconsole_configurationFactory_nameHint() default "Controller IO FixDigitalOutput [{id}]";

}