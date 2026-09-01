package io.openems.edge.meter.mqtt;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;
import io.openems.edge.meter.api.PhaseRotation;

@ObjectClassDefinition(//
		name = "Meter MQTT", //
		description = "Implements a generic, config-driven electricity meter that maps a flat JSON MQTT payload "
				+ "to ElectricityMeter channels")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Meter-Type", description = "Grid, Production, Consumption-Metered (default)")
	MeterType type() default MeterType.CONSUMPTION_METERED;

	@AttributeDefinition(name = "Phase rotation", description = "The way in which the phases are physically rotated.")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	@AttributeDefinition(name = "MQTT Bridge ID", description = "ID of the MQTT Bridge component (e.g., mqtt0)")
	String mqttBridgeId() default "mqtt0";

	@AttributeDefinition(name = "Topic", description = "The MQTT topic filter to subscribe to "
			+ "(e.g., openami/StreetPoleEMS_37EAB0/meter_0)")
	String topic() default "";

	@AttributeDefinition(name = "Mapping", description = "Maps JSON fields to ElectricityMeter channels. "
			+ "Each entry is \"jsonField:CHANNEL:scale\", where CHANNEL is an ElectricityMeter Channel-ID "
			+ "(e.g., ACTIVE_POWER) and scale is a numeric factor applied to the JSON value "
			+ "(e.g., \"PhV:VOLTAGE:1000\" converts Volt to Millivolt).")
	String[] mapping() default {};

	String webconsole_configurationFactory_nameHint() default "Meter MQTT [{id}]";

}
