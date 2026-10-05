package io.openems.edge.meter.meshems.threephase;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;

@ObjectClassDefinition(//
		name = "Meter MeshEMS Three-Phase", //
		description = "Implements the MeshEMS OpenAMI three-phase subpanel meter (SunSpec model 213) published as JSON "
				+ "over MQTT. The SunSpec-field-to-Channel mapping is baked into code.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Meter-Type", description = "Grid, Production, Consumption-Metered (default)")
	MeterType type() default MeterType.CONSUMPTION_METERED;

	@AttributeDefinition(name = "Invert Power", description = "Inverts all Power values and swaps production and "
			+ "consumption energy, i.e. Power is multiplied with -1.")
	boolean invert() default false;

	@AttributeDefinition(name = "MQTT Bridge ID", description = "ID of the MQTT Bridge component (e.g., mqtt0)")
	String mqttBridgeId() default "mqtt0";

	@AttributeDefinition(name = "Topic", description = "The MQTT topic filter to subscribe to "
			+ "(e.g., openami/StreetPoleEMS_37EAB0/subpanel_3Ph)")
	String topic() default "";

	String webconsole_configurationFactory_nameHint() default "Meter MeshEMS Three-Phase [{id}]";
}
