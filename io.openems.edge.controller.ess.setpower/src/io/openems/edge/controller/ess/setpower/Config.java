package io.openems.edge.controller.ess.setpower;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller io.openems.edge.controller.ess.setpower", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "setpower0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Input Channel", description = "Address of the input channel. If the value of this channel is within a configured threshold, the output channel is switched ON.")
	String inputChannelAddress();

	String webconsole_configurationFactory_nameHint() default "Controller Set Power [{id}]";

}
