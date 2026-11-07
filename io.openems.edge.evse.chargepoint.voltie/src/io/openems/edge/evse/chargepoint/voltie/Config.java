package io.openems.edge.evse.chargepoint.voltie;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.chargepoint.voltie.enums.LogVerbosity;
import io.openems.edge.meter.api.PhaseRotation;

@ObjectClassDefinition(name = "EVSE Charge-Point Voltie", //
		description = "The Voltie AC electric vehicle charging station (via Modbus/TCP gateway on port 502)")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evseChargePoint0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read only", description = "Defines that this charge-point is read only.")
	boolean readOnly() default false;

	@AttributeDefinition(name = "Hardware Wiring", description = "Is the charge-point connected single-phase or three-phase?")
	SingleOrThreePhase wiring() default SingleOrThreePhase.THREE_PHASE;

	@AttributeDefinition(name = "Phase Rotation", description = "Apply standard or rotated wiring")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	@AttributeDefinition(name = "Log-Verbosity", description = "The log verbosity.")
	LogVerbosity logVerbosity() default LogVerbosity.NONE;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device. Voltie default is 11; Unit-ID 0 must not be used.")
	int modbusUnitId() default 11;

	String webconsole_configurationFactory_nameHint() default "EVSE Charge-Point Voltie [{id}]";
}
