package io.openems.edge.evse.chargepoint.abl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.meter.api.PhaseRotation;

@ObjectClassDefinition(name = "EVSE Charge-Point ABL", //
		description = "The ABL EVCC2/3 electric vehicle charging station")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evseChargePoint0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read only", description = "Defines that this evse is read only.", required = true)
	boolean readOnly() default false;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	@AttributeDefinition(name = "Hardware Wiring", description = "Actual wiring configuration (single or three-phase)", required = true)
	SingleOrThreePhase wiring() default SingleOrThreePhase.THREE_PHASE;

	@AttributeDefinition(name = "Phase Rotation", description = "Apply standard or rotated wiring")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device (default: 1)")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Maximum Current", description = "Maximum supported current in Ampere (default: 32A)")
	int maxCurrent() default 32;

	String webconsole_configurationFactory_nameHint() default "EVSE Charge-Point ABL [{id}]";
}
