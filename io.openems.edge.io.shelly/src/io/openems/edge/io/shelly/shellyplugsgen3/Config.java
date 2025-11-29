package io.openems.edge.io.shelly.shellyplugsgen3;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.DebugMode;
import io.openems.common.types.MeterType;
import io.openems.edge.common.type.Phase.SinglePhase;

@ObjectClassDefinition(//
		name = "IO Shelly Plug S Gen3", //
		description = "Implements the Shelly Plug S  Gen3")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "io0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Phase", description = "Which Phase is this Shelly Plug connected to?")
	SinglePhase phase() default SinglePhase.L1;

	@AttributeDefinition(name = "MDNS Name")
	String mdnsName();

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the Shelly device.")
	String ip();

	@AttributeDefinition(name = "Meter-Type", description = "What is measured by this Meter?")
	MeterType type() default MeterType.CONSUMPTION_METERED;

	@AttributeDefinition(name = "Invert Power", description = "Inverts all Power values, inverts current values, swaps production and consumptioon energy, i.e. Power is multiplied with -1.")
	boolean invert() default false;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	DebugMode debugMode() default DebugMode.OFF;

	String webconsole_configurationFactory_nameHint() default "IO Shelly Plug S Gen3 [{id}]";
}