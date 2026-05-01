package io.openems.edge.io.shelly.shellypro4pm;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.DebugMode;
import io.openems.edge.common.type.Phase.SinglePhase;

@ObjectClassDefinition(//
		name = "IO Shelly Pro 4PM Device", //
		description = "Implements the Shelly Pro 4PM")
@interface DeviceConfig {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "io0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Phase", description = "Which Phase is this Shelly connected to?")
	SinglePhase phase() default SinglePhase.L1;

	@AttributeDefinition(name = "MDNS Name", required = false)
	String mdnsName();

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the Shelly device.", required = false)
	String ip();

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode.")
	DebugMode debugMode() default DebugMode.OFF;

	String webconsole_configurationFactory_nameHint() default "IO Shelly Pro 4PM Device [{id}]";
}