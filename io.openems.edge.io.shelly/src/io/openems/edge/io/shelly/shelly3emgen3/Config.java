package io.openems.edge.io.shelly.shelly3emgen3;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.DebugMode;
import io.openems.common.types.MeterType;

@ObjectClassDefinition(name = "IO Shelly 3EM Gen3", //
		description = "Implements the Shelly 3EM Gen3 Energy Meter.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Meter-Type", description = "Grid, Production (=default), Consumption")
	MeterType type() default MeterType.GRID;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the Shelly device.", required = false)
	String ip();

	@AttributeDefinition(name = "MDNS Name", required = false)
	String mdnsName() default "";

	@AttributeDefinition(name = "Invert Power", description = "Inverts all Power values, inverts current values, swaps production and consumptioon energy, i.e. Power is multiplied with -1.")
	boolean invert() default false;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode.")
	DebugMode debugMode() default DebugMode.OFF;

	String webconsole_configurationFactory_nameHint() default "IO Shelly 3EM Gen3 [{id}]";

}