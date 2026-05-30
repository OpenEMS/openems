package io.openems.edge.meter.chint.dtsu666;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;
import io.openems.edge.meter.api.PhaseRotation;

@ObjectClassDefinition(//
		name = "Meter Chint DTSU666", //
		description = "Implements the Chint DTSU666 three-phase, four-wire energy meter via Modbus/RTU or Modbus/TCP")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Meter-Type", description = "Grid (default), Production, Consumption")
	MeterType type() default MeterType.GRID;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device (slave address).")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Invert Power", description = "Inverts ALL Power values and swaps production/consumption energy. Use when meter is wired in reverse.")
	boolean invert() default false;

	@AttributeDefinition(name = "Phase-Rotation", description = "Phase rotation of the measured three-phase system.")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	String webconsole_configurationFactory_nameHint() default "Meter Chint DTSU666 [{id}]";
}
