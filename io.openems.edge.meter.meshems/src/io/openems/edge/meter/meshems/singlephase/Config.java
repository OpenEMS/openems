package io.openems.edge.meter.meshems.singlephase;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;
import io.openems.edge.common.type.Phase.SinglePhase;

@ObjectClassDefinition(//
		name = "Meter MeshEMS Single-Phase", //
		description = "Implements the MeshEMS OpenAMI single-phase tenant meter (SunSpec model 11) published as JSON "
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

	@AttributeDefinition(name = "Phase", description = "Which Phase is measured by this Meter?")
	SinglePhase phase() default SinglePhase.L1;

	@AttributeDefinition(name = "Invert Power", description = "Inverts all Power values and swaps production and "
			+ "consumption energy, i.e. Power is multiplied with -1.")
	boolean invert() default false;

	@AttributeDefinition(name = "MQTT Bridge ID", description = "ID of the MQTT Bridge component (e.g., mqtt0)")
	String mqttBridgeId() default "mqtt0";

	@AttributeDefinition(name = "Topic", description = "The MQTT topic filter to subscribe to "
			+ "(e.g., openami/StreetPoleEMS_37EAB0/meter_0)")
	String topic() default "";

	@AttributeDefinition(name = "Stale timeout [s]", description = "Seconds without a message before values are "
			+ "cleared; board publishes every ~30 s")
	int staleTimeoutSeconds() default 90;

	String webconsole_configurationFactory_nameHint() default "Meter MeshEMS Single-Phase [{id}]";
}
