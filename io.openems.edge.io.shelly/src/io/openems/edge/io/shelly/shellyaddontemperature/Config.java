package io.openems.edge.io.shelly.shellyaddontemperature;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "IO Shelly Add-on Temperature", //
		description = "Implements a temperature sensor connected to a Shelly via the Shelly Plus Add-on.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "temp0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the Shelly device.")
	String ip();

	@AttributeDefinition(name = "Sensor-ID", description = "ID of the temperature sensor on the Shelly, e.g. 100 for the first sensor of a Shelly Plus Add-on.")
	int sensor_id() default 100;

	String webconsole_configurationFactory_nameHint() default "IO Shelly Add-on Temperature [{id}]";
}
