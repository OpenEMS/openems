package io.openems.edge.evcs.openwb;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.PhaseRotation;

@ObjectClassDefinition(//
		name = "EVCS OpenWB", //
		description = "Implements the EVCS component for OpenWB Series2 with internal chargepoints via MQTT")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcs0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "MQTT Bridge ID", description = "ID of the MQTT Bridge component (e.g., mqtt0)")
	String mqttBridgeId() default "mqtt0";

	@AttributeDefinition(name = "Chargepoint", description = "Number of the internal chargepoint (duo_num)")
	ChargePoint chargePoint() default ChargePoint.CP0;

	@AttributeDefinition(name = "Phase rotation", description = "The way in which the phases are physically rotated.")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	String webconsole_configurationFactory_nameHint() default "EVCS OpenWB [{id}]";

}
