package io.openems.edge.io.shelly.shellyplus1pmaddon;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@ObjectClassDefinition(//
		name = "IO Shelly Plus 1PM AddOn Input", //
		description = "Implements the Shelly AddOn Channel")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ioShellyInput0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the Shelly device.")
	String ip();

	@AttributeDefinition(name = "Channel-Type", description = "What type of sensor is attached to this channel?")
	AddOnEnums.InputType type() default AddOnEnums.InputType.TEMPERATURE;

	@AttributeDefinition(name = "Channel-Index", description = "Index of this channel?")
	AddOnEnums.InputIndex index() default AddOnEnums.InputIndex.Index100;
	

	String webconsole_configurationFactory_nameHint() default "IO Shelly Plus 1PM AddOn Channel[{id}]";
}
